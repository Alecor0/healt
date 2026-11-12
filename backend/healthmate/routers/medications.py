from datetime import datetime, timezone
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import AwareDatetime
from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError

from ..dependencies import CurrentUser, Db, no_store
from ..health_schemas import (
    FINAL_DOSE_STATES,
    DoseOut,
    DoseUpdate,
    MedicationCreate,
    MedicationOut,
    MedicationState,
)
from ..models import Dose, MedicationCatalog, ReminderSchedule, UserMedication
from ..profiles import set_audit_user

router = APIRouter(prefix="/medicamentos", tags=["medicamentos"], dependencies=[Depends(no_store)])


def _medications(user_id: UUID):
    """Medicamentos no eliminados del usuario, con su nombre y su recordatorio."""
    return (
        select(UserMedication, MedicationCatalog.nombre, ReminderSchedule)
        .join(MedicationCatalog, MedicationCatalog.id == UserMedication.medicamento_id)
        .join(ReminderSchedule, ReminderSchedule.usuario_medicamento_id == UserMedication.id)
        .where(UserMedication.usuario_id == user_id, UserMedication.eliminado_en.is_(None))
    )


def _to_out(medication: UserMedication, nombre: str, reminder: ReminderSchedule) -> MedicationOut:
    return MedicationOut(
        id=medication.id,
        recordatorio_id=reminder.id,
        nombre=nombre,
        dosis=medication.dosis or "",
        frecuencia=medication.frecuencia or "",
        via_administracion=medication.via_administracion or "",
        hora=reminder.hora,
        fecha_inicio=medication.fecha_inicio,
        fecha_fin=medication.fecha_fin,
        activo=medication.activo,
        proposito=medication.proposito or "",
    )


async def _owned(db: Db, user_id: UUID, medication_id: UUID):
    row = (await db.execute(_medications(user_id).where(UserMedication.id == medication_id))).first()
    if row is None:
        raise HTTPException(404, "Medicamento no encontrado")
    return row


@router.get("")
async def list_medications(user: CurrentUser, db: Db) -> list[MedicationOut]:
    rows = await db.execute(_medications(user.id).order_by(ReminderSchedule.hora, MedicationCatalog.nombre))
    return [_to_out(*row) for row in rows]


@router.post("", status_code=201)
async def add_medication(body: MedicationCreate, user: CurrentUser, db: Db) -> MedicationOut:
    await set_audit_user(db, user.id)
    catalog_id = await db.scalar(
        select(MedicationCatalog.id)
        .where(
            func.lower(MedicationCatalog.nombre) == body.nombre.lower(),
            MedicationCatalog.compuesto_activo.is_(None),
        )
        .limit(1)
    )
    if catalog_id is None:
        entry = MedicationCatalog(nombre=body.nombre)
        db.add(entry)
        await db.flush()
        catalog_id = entry.id
    medication = UserMedication(
        usuario_id=user.id,
        medicamento_id=catalog_id,
        dosis=body.dosis,
        frecuencia=body.frecuencia,
        via_administracion=body.via_administracion,
        fecha_inicio=body.fecha_inicio,
        fecha_fin=body.fecha_fin,
        proposito=body.proposito or None,
        activo=True,
    )
    db.add(medication)
    await db.flush()
    # Invariante 5: el recordatorio siempre pertenece a un medicamento del usuario.
    reminder = ReminderSchedule(usuario_medicamento_id=medication.id, hora=body.hora, activo=True)
    db.add(reminder)
    await db.flush()
    result = _to_out(medication, body.nombre, reminder)
    await db.commit()
    return result


@router.patch("/{medication_id}")
async def set_medication_state(
    medication_id: UUID, body: MedicationState, user: CurrentUser, db: Db
) -> MedicationOut:
    """Pausa o reanuda un medicamento y sus recordatorios."""
    await set_audit_user(db, user.id)
    medication, nombre, reminder = await _owned(db, user.id, medication_id)
    medication.activo = body.activo
    reminder.activo = body.activo
    await db.flush()
    result = _to_out(medication, nombre, reminder)
    await db.commit()
    return result


@router.delete("/{medication_id}", status_code=204)
async def delete_medication(medication_id: UUID, user: CurrentUser, db: Db) -> None:
    """Borrado lógico: el historial de dosis se conserva (caso límite 4.5)."""
    await set_audit_user(db, user.id)
    medication, _, _ = await _owned(db, user.id, medication_id)
    medication.activo = False
    medication.eliminado_en = datetime.now(timezone.utc)
    await db.execute(
        update(ReminderSchedule)
        .where(ReminderSchedule.usuario_medicamento_id == medication.id)
        .values(activo=False)
    )
    await db.commit()


@router.get("/dosis")
async def list_doses(
    user: CurrentUser,
    db: Db,
    desde: Annotated[AwareDatetime, Query()],
    hasta: Annotated[AwareDatetime, Query()],
) -> list[DoseOut]:
    """Dosis registradas del usuario con hora programada en [desde, hasta)."""
    rows = await db.scalars(
        select(Dose)
        .join(ReminderSchedule, ReminderSchedule.id == Dose.recordatorio_id)
        .join(UserMedication, UserMedication.id == ReminderSchedule.usuario_medicamento_id)
        .where(
            UserMedication.usuario_id == user.id,
            Dose.fecha_programada >= desde,
            Dose.fecha_programada < hasta,
        )
        .order_by(Dose.fecha_programada)
    )
    return [_dose_out(dose) for dose in rows]


def _dose_out(dose: Dose) -> DoseOut:
    return DoseOut(
        recordatorio_id=dose.recordatorio_id,
        fecha_programada=dose.fecha_programada,
        estado=dose.estado,
        pospuesto_minutos=dose.pospuesto_minutos,
    )


@router.put("/dosis")
async def record_dose(body: DoseUpdate, user: CurrentUser, db: Db) -> DoseOut:
    """Marca una dosis programada como tomada, omitida o pospuesta (RF-03)."""
    await set_audit_user(db, user.id)
    owned = await db.scalar(
        select(ReminderSchedule.id)
        .join(UserMedication, UserMedication.id == ReminderSchedule.usuario_medicamento_id)
        .where(
            ReminderSchedule.id == body.recordatorio_id,
            UserMedication.usuario_id == user.id,
            UserMedication.eliminado_en.is_(None),
        )
    )
    if owned is None:
        raise HTTPException(404, "Recordatorio no encontrado")

    dose = await db.scalar(
        select(Dose)
        .where(Dose.recordatorio_id == body.recordatorio_id, Dose.fecha_programada == body.fecha_programada)
        .with_for_update()
    )
    if dose is not None and dose.estado in FINAL_DOSE_STATES:
        raise HTTPException(409, "La dosis ya fue registrada y no se puede modificar")
    if dose is None:
        dose = Dose(recordatorio_id=body.recordatorio_id, fecha_programada=body.fecha_programada)
        db.add(dose)
    dose.estado = body.estado
    if body.estado == "pospuesto":
        dose.pospuesto_minutos = body.pospuesto_minutos
    if body.estado == "tomado":
        # La tabla exige confirmación >= hora programada: una toma adelantada se anota a su hora.
        dose.fecha_confirmacion = max(datetime.now(timezone.utc), body.fecha_programada)
    try:
        await db.flush()
    except IntegrityError as exc:
        # Otra petición registró la misma dosis a la vez.
        raise HTTPException(409, "La dosis ya fue registrada y no se puede modificar") from exc
    await db.refresh(dose)
    result = _dose_out(dose)
    await db.commit()
    return result
