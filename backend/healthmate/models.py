from datetime import date, datetime
from decimal import Decimal
from uuid import UUID

from sqlalchemy import CheckConstraint, ForeignKey, UniqueConstraint, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.types import DateTime, Numeric, String


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "usuarios"
    __table_args__ = (
        UniqueConstraint("email", name="uq_usuarios_email"),
        UniqueConstraint("google_sub", name="uq_usuarios_google_sub"),
        CheckConstraint(
            "password_hash IS NOT NULL OR google_sub IS NOT NULL",
            name="ck_usuarios_credencial",
        ),
        CheckConstraint("email = lower(email)", name="ck_usuarios_email_normalizado"),
        CheckConstraint(
            "rol IN ('paciente', 'cuidador', 'admin')", name="ck_usuarios_rol"
        ),
        CheckConstraint(
            "peso > 0 AND peso <> 'NaN'::numeric", name="ck_usuarios_peso"
        ),
        CheckConstraint("altura > 0", name="ck_usuarios_altura"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    nombre: Mapped[str] = mapped_column(String(100))
    email: Mapped[str] = mapped_column(String(255))
    password_hash: Mapped[str | None] = mapped_column(String(255))
    google_sub: Mapped[str | None] = mapped_column(String(255))
    rol: Mapped[str] = mapped_column(String(10), server_default="paciente")
    creado_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    fecha_nacimiento: Mapped[date]
    peso: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    altura: Mapped[int | None]
    alergias: Mapped[str] = mapped_column(String(1000), server_default="")
    condiciones_cronicas: Mapped[str] = mapped_column(String(1000), server_default="")
    contacto_emergencia: Mapped[str] = mapped_column(String(100), server_default="")


class RefreshSession(Base):
    __tablename__ = "sesiones_refresh"

    id: Mapped[UUID] = mapped_column(primary_key=True)
    usuario_id: Mapped[int] = mapped_column(
        ForeignKey("usuarios.id", ondelete="CASCADE"), index=True
    )
    vence_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)


class GoogleChallenge(Base):
    __tablename__ = "desafios_google"

    nonce: Mapped[UUID] = mapped_column(primary_key=True)
    vence_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
