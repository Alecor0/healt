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
    alergias: str = Field(default="", max_length=1000)
    condiciones_cronicas: str = Field(default="", max_length=1000)
    contacto_emergencia: str = Field(default="", max_length=100)


class Profile(ProfileUpdate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str

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
