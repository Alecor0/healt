from datetime import date
from decimal import Decimal
from typing import Annotated
from uuid import UUID

from pydantic import (
    AfterValidator,
    BaseModel,
    ConfigDict,
    EmailStr,
    Field,
    SecretStr,
    StringConstraints,
    field_serializer,
    field_validator,
)

Name = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)]


def validate_birth_date(value: date) -> date:
    if value > date.today():
        raise ValueError("La fecha de nacimiento no puede ser futura")
    return value


BirthDate = Annotated[date, AfterValidator(validate_birth_date)]


def split_items(value: str) -> list[str]:
    """Separa una lista escrita a mano ("Penicilina, Polen") en elementos únicos."""
    items: list[str] = []
    seen: set[str] = set()
    for raw in value.replace(";", ",").replace("\n", ",").split(","):
        item = " ".join(raw.split())
        if item and item.casefold() not in seen:
            seen.add(item.casefold())
            items.append(item)
    return items


def validate_allergies(value: str) -> str:
    # Cada alergia se guarda en el catálogo `alergias`, cuyo nombre admite 100 caracteres.
    if any(len(item) > 100 for item in split_items(value)):
        raise ValueError("Cada alergia admite como máximo 100 caracteres")
    return value


class Login(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr = Field(max_length=254)
    password: SecretStr = Field(min_length=1, max_length=128)

    @field_validator("email", mode="after")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.lower()


class Registration(Login):
    nombre: Name
    fecha_nacimiento: BirthDate
    password: SecretStr = Field(min_length=12, max_length=128)


class ProfileUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    nombre: Name
    fecha_nacimiento: BirthDate
    peso: Decimal | None = Field(default=None, gt=0, max_digits=5, decimal_places=2)
    altura: int | None = Field(default=None, gt=0, le=2_147_483_647, strict=True)
    alergias: Annotated[str, AfterValidator(validate_allergies)] = Field(default="", max_length=1000)
    condiciones_cronicas: str = Field(default="", max_length=1000)
    contacto_emergencia: str = Field(default="", max_length=100)


class Profile(ProfileUpdate):
    id: UUID
    email: str
    # Puede faltar en perfiles creados fuera de la API.
    fecha_nacimiento: date | None = None

    @field_serializer("peso")
    def serialize_weight(self, value: Decimal | None) -> str | None:
        return format(value, ".2f") if value is not None else None


class Tokens(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    usuario: Profile


class RefreshRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    refresh_token: SecretStr = Field(min_length=1, max_length=2048)


class GoogleChallengeResponse(BaseModel):
    nonce: UUID


class GoogleLogin(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id_token: SecretStr = Field(min_length=1, max_length=8192)
    fecha_nacimiento: BirthDate | None = None
