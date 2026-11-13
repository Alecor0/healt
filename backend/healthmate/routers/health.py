from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select

from ..dependencies import CurrentUser, Db, no_store
from ..health_schemas import HealthRecordCreate, HealthRecordOut
from ..models import VitalSigns
from ..profiles import set_audit_user

router = APIRouter(prefix="/salud", tags=["salud"], dependencies=[Depends(no_store)])
# Suficiente para las tendencias semanales y mensuales (RF-06).
HISTORY_LIMIT = 200


def _to_out(record: VitalSigns) -> HealthRecordOut:
    return HealthRecordOut(
        id=record.id,
        fecha_registro=record.fecha_registro,
        dolor=record.dolor,
        estado_animo=record.estado_animo,
        presion_sistolica=record.presion_sistolica,
        presion_diastolica=record.presion_diastolica,
        glucosa=record.glucosa,
        peso=record.peso,
        pulso=record.frecuencia_cardiaca,
        notas=record.notas or "",
    )


@router.get("/registros")
async def list_records(user: CurrentUser, db: Db) -> list[HealthRecordOut]:
    """Registros del usuario, del más reciente al más antiguo."""
    rows = await db.scalars(
        select(VitalSigns)
        .where(VitalSigns.paciente_id == user.id)
        .order_by(VitalSigns.fecha_registro.desc())
        .limit(HISTORY_LIMIT)
    )
    return [_to_out(record) for record in rows]


@router.post("/registros", status_code=201)
async def add_record(body: HealthRecordCreate, user: CurrentUser, db: Db) -> HealthRecordOut:
    await set_audit_user(db, user.id)
    record = VitalSigns(
        paciente_id=user.id,
        presion_sistolica=body.presion_sistolica,
        presion_diastolica=body.presion_diastolica,
        frecuencia_cardiaca=body.pulso,
        glucosa=round(body.glucosa, 2),
        peso=round(body.peso, 2),
        dolor=body.dolor,
        estado_animo=body.estado_animo,
        notas=body.notas or None,
        fecha_registro=body.fecha_registro or datetime.now(timezone.utc),
    )
    db.add(record)
    await db.flush()
    result = _to_out(record)
    await db.commit()
    return result
