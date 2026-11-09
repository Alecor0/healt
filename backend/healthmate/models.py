"""Modelos mapeados al esquema existente en Neon (usuarios, perfiles, roles...)."""
from datetime import date, datetime
from decimal import Decimal
from uuid import UUID, uuid4

from sqlalchemy import CheckConstraint, ForeignKey, func, true
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.types import DateTime, Numeric, String, Text


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "usuarios"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    email: Mapped[str] = mapped_column(String(255), unique=True)
    # Obligatorio en el esquema: las cuentas de Google guardan un hash aleatorio inutilizable.
    password_hash: Mapped[str] = mapped_column(String(255))
    estado: Mapped[bool] = mapped_column(server_default=true())
    fecha_creacion: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), server_default=func.current_timestamp()
    )
    google_id: Mapped[str | None] = mapped_column(String(255), unique=True)


class Role(Base):
    __tablename__ = "roles"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    nombre: Mapped[str] = mapped_column(String(50), unique=True)


class UserRole(Base):
    __tablename__ = "usuario_rol"

    usuario_id: Mapped[UUID] = mapped_column(
        ForeignKey("usuarios.id", ondelete="CASCADE"), primary_key=True
    )
    rol_id: Mapped[UUID] = mapped_column(ForeignKey("roles.id", ondelete="CASCADE"), primary_key=True)


class Perfil(Base):
    __tablename__ = "perfiles"
    __table_args__ = (
        CheckConstraint("peso > 0 AND peso <> 'NaN'::numeric", name="ck_perfiles_peso"),
        CheckConstraint("altura > 0", name="ck_perfiles_altura"),
    )

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    usuario_id: Mapped[UUID] = mapped_column(
        ForeignKey("usuarios.id", ondelete="CASCADE"), unique=True
    )
    nombre_completo: Mapped[str | None] = mapped_column(String(255))
    telefono: Mapped[str | None] = mapped_column(String(20))
    fecha_nacimiento: Mapped[date | None]
    tipo_sangre: Mapped[str | None] = mapped_column(String(5))
    peso: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    altura: Mapped[int | None]
    contacto_emergencia: Mapped[str | None] = mapped_column(String(100))


class Allergy(Base):
    __tablename__ = "alergias"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    nombre: Mapped[str] = mapped_column(String(100), unique=True)
    categoria: Mapped[str | None] = mapped_column(String(50))
    descripcion: Mapped[str | None] = mapped_column(Text)


class ProfileAllergy(Base):
    __tablename__ = "perfil_alergias"
    __table_args__ = (
        CheckConstraint(
            "severidad IN ('leve', 'moderada', 'grave')", name="perfil_alergias_severidad_check"
        ),
    )

    perfil_id: Mapped[UUID] = mapped_column(
        ForeignKey("perfiles.id", ondelete="CASCADE"), primary_key=True
    )
    alergia_id: Mapped[UUID] = mapped_column(
        ForeignKey("alergias.id", ondelete="CASCADE"), primary_key=True
    )
    severidad: Mapped[str | None] = mapped_column(String(50))


class MedicalHistory(Base):
    __tablename__ = "antecedentes"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    perfil_id: Mapped[UUID] = mapped_column(ForeignKey("perfiles.id", ondelete="CASCADE"))
    tipo: Mapped[str | None] = mapped_column(String(50))
    descripcion: Mapped[str | None] = mapped_column(Text)
    fecha_registro: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), server_default=func.current_timestamp()
    )


class RefreshSession(Base):
    __tablename__ = "sesiones_refresh"

    id: Mapped[UUID] = mapped_column(primary_key=True)
    usuario_id: Mapped[UUID] = mapped_column(
        ForeignKey("usuarios.id", ondelete="CASCADE"), index=True
    )
    vence_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)


class GoogleChallenge(Base):
    __tablename__ = "desafios_google"

    nonce: Mapped[UUID] = mapped_column(primary_key=True)
    vence_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
