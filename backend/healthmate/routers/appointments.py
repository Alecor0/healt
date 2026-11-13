from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select

from ..dependencies import CurrentUser, Db, no_store
from ..health_schemas import AppointmentCreate, AppointmentOut, AppointmentState
from ..models import Appointment
from ..profiles import set_audit_user

router = APIRouter(prefix="/citas", tags=["citas"], dependencies=[Depends(no_store)])


def _to_out(appointment: Appointment) -> AppointmentOut:
    return AppointmentOut(
        id=appointment.id,
        especialista=appointment.especialista or "",
        especialidad=appointment.especialidad or "",
        lugar=appointment.lugar or "",
        fecha_hora=appointment.fecha_hora,
        estado=appointment.estado,
        notas=appointment.motivo or "",
    )


@router.get("")
async def list_appointments(user: CurrentUser, db: Db) -> list[AppointmentOut]:
    rows = await db.scalars(
        select(Appointment).where(Appointment.paciente_id == user.id).order_by(Appointment.fecha_hora)
    )
    return [_to_out(appointment) for appointment in rows]


@router.post("", status_code=201)
async def add_appointment(body: AppointmentCreate, user: CurrentUser, db: Db) -> AppointmentOut:
    await set_audit_user(db, user.id)
    appointment = Appointment(
        paciente_id=user.id,
        especialista=body.especialista,
        especialidad=body.especialidad or None,
        lugar=body.lugar,
        fecha_hora=body.fecha_hora,
        motivo=body.notas or None,
        estado="confirmada",
    )
    db.add(appointment)
    await db.flush()
    result = _to_out(appointment)
    await db.commit()
    return result


@router.patch("/{appointment_id}")
async def cancel_appointment(
    appointment_id: UUID, body: AppointmentState, user: CurrentUser, db: Db
) -> AppointmentOut:
    await set_audit_user(db, user.id)
    appointment = await db.scalar(
        select(Appointment).where(Appointment.id == appointment_id, Appointment.paciente_id == user.id)
    )
    if appointment is None:
        raise HTTPException(404, "Cita no encontrada")
    appointment.estado = body.estado
    await db.flush()
    result = _to_out(appointment)
    await db.commit()
    return result
