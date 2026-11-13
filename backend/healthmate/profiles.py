"""Lectura y escritura del perfil médico repartido en las tablas del esquema Neon."""
from datetime import date
from uuid import UUID

from sqlalchemy import delete, func, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.ext.asyncio import AsyncSession

from .models import Allergy, MedicalHistory, Perfil, ProfileAllergy, Role, User, UserRole
from .schemas import Profile, ProfileUpdate, split_items

CHRONIC = "condicion_cronica"
DEFAULT_ROLE = "paciente"


async def set_audit_user(db: AsyncSession, user_id: UUID) -> None:
    """Indica a los triggers de auditoría quién hace los cambios de esta transacción."""
    await db.execute(select(func.set_config("app.current_user_id", str(user_id), True)))


async def create_account(
    db: AsyncSession,
    *,
    email: str,
    password_hash: str,
    nombre: str,
    fecha_nacimiento: date,
    google_id: str | None = None,
) -> User | None:
    """Crea usuario, perfil y rol paciente. Devuelve None si el correo o la cuenta de Google ya existen."""
    user = await db.scalar(
        insert(User)
        .values(email=email, password_hash=password_hash, google_id=google_id)
        .on_conflict_do_nothing()
        .returning(User)
    )
    if user is None:
        return None
    await set_audit_user(db, user.id)
    db.add(Perfil(usuario_id=user.id, nombre_completo=nombre, fecha_nacimiento=fecha_nacimiento))
    role_id = await db.scalar(select(Role.id).where(Role.nombre == DEFAULT_ROLE))
    if role_id is None:
        raise RuntimeError(f"Falta el rol '{DEFAULT_ROLE}' en la tabla roles")
    db.add(UserRole(usuario_id=user.id, rol_id=role_id))
    await db.flush()
    return user


async def _profile_row(db: AsyncSession, user_id: UUID) -> Perfil:
    perfil = await db.scalar(select(Perfil).where(Perfil.usuario_id == user_id))
    if perfil is None:
        # Cuenta creada fuera de la API: se le crea un perfil vacío.
        perfil = Perfil(usuario_id=user_id)
        db.add(perfil)
        await db.flush()
    return perfil


async def _allergy_names(db: AsyncSession, perfil_id: UUID) -> list[str]:
    rows = await db.scalars(
        select(Allergy.nombre)
        .join(ProfileAllergy, ProfileAllergy.alergia_id == Allergy.id)
        .where(ProfileAllergy.perfil_id == perfil_id)
        .order_by(Allergy.nombre)
    )
    return list(rows)


async def _chronic_conditions(db: AsyncSession, perfil_id: UUID) -> list[MedicalHistory]:
    rows = await db.scalars(
        select(MedicalHistory)
        .where(MedicalHistory.perfil_id == perfil_id, MedicalHistory.tipo == CHRONIC)
        .order_by(MedicalHistory.descripcion)
    )
    return list(rows)


async def load_profile(db: AsyncSession, user: User) -> Profile:
    perfil = await _profile_row(db, user.id)
    conditions = await _chronic_conditions(db, perfil.id)
    return Profile(
        id=user.id,
        email=user.email,
        nombre=perfil.nombre_completo or user.email.split("@", 1)[0],
        fecha_nacimiento=perfil.fecha_nacimiento,
        peso=perfil.peso,
        altura=perfil.altura,
        alergias=", ".join(await _allergy_names(db, perfil.id)),
        condiciones_cronicas=", ".join(c.descripcion or "" for c in conditions),
        contacto_emergencia=perfil.contacto_emergencia or "",
    )


async def save_profile(db: AsyncSession, user: User, body: ProfileUpdate) -> None:
    """Actualiza el perfil. Solo inserta o borra las alergias y condiciones que cambiaron."""
    await set_audit_user(db, user.id)
    perfil = await _profile_row(db, user.id)
    perfil.nombre_completo = body.nombre
    perfil.fecha_nacimiento = body.fecha_nacimiento
    perfil.peso = body.peso
    perfil.altura = body.altura
    perfil.contacto_emergencia = body.contacto_emergencia or None

    await _sync_allergies(db, perfil.id, split_items(body.alergias))
    await _sync_conditions(db, perfil.id, split_items(body.condiciones_cronicas))


async def _sync_allergies(db: AsyncSession, perfil_id: UUID, wanted: list[str]) -> None:
    if wanted:
        await db.execute(
            insert(Allergy).values([{"nombre": name} for name in wanted]).on_conflict_do_nothing()
        )
    wanted_ids = set(await db.scalars(select(Allergy.id).where(Allergy.nombre.in_(wanted)))) if wanted else set()
    current_ids = set(
        await db.scalars(select(ProfileAllergy.alergia_id).where(ProfileAllergy.perfil_id == perfil_id))
    )
    if current_ids - wanted_ids:
        await db.execute(
            delete(ProfileAllergy).where(
                ProfileAllergy.perfil_id == perfil_id,
                ProfileAllergy.alergia_id.in_(current_ids - wanted_ids),
            )
        )
    for alergia_id in wanted_ids - current_ids:
        db.add(ProfileAllergy(perfil_id=perfil_id, alergia_id=alergia_id))


async def _sync_conditions(db: AsyncSession, perfil_id: UUID, wanted: list[str]) -> None:
    current = await _chronic_conditions(db, perfil_id)
    current_names = {c.descripcion for c in current}
    for condition in current:
        if condition.descripcion not in wanted:
            await db.delete(condition)
    for name in wanted:
        if name not in current_names:
            db.add(MedicalHistory(perfil_id=perfil_id, tipo=CHRONIC, descripcion=name))
