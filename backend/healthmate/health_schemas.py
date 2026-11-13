"""Esquemas de medicamentos (RF-02), dosis (RF-03), citas (RF-04) y registros de salud (RF-05)."""
from datetime import date, datetime, time, timezone
from typing import Annotated, Literal
from uuid import UUID

from pydantic import (
    AfterValidator,
    AwareDatetime,
    BaseModel,
    ConfigDict,
    Field,
    StringConstraints,
    field_serializer,
    model_validator,
)

Required100 = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)]
Required255 = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=255)]
Notes = Annotated[str, StringConstraints(strip_whitespace=True, max_length=1000)]

DoseState = Literal["tomado", "omitido", "pospuesto"]
# Una dosis tomada u omitida ya no se modifica (invariante 7).
FINAL_DOSE_STATES = ("tomado", "omitido")
Mood = Literal["triste", "normal", "feliz", "emocionado"]


class Strict(BaseModel):
    model_config = ConfigDict(extra="forbid")


class MedicationCreate(Strict):
    nombre: Required255
    dosis: Required100
    frecuencia: Required100
    via_administracion: Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=50)] = "Oral"
    hora: time
    fecha_inicio: date
    fecha_fin: date | None = None
    proposito: Annotated[str, StringConstraints(strip_whitespace=True, max_length=255)] = ""

    @model_validator(mode="after")
    def end_after_start(self):
        # Invariante 4: fecha_fin >= fecha_inicio.
        if self.fecha_fin is not None and self.fecha_fin < self.fecha_inicio:
            raise ValueError("La fecha de fin no puede ser anterior a la de inicio")
        return self


class MedicationState(Strict):
    activo: bool


class MedicationOut(BaseModel):
    id: UUID
    recordatorio_id: UUID
    nombre: str
    dosis: str
    frecuencia: str
    via_administracion: str
    hora: time
    fecha_inicio: date | None
    fecha_fin: date | None
    activo: bool
    proposito: str

    @field_serializer("hora")
    def serialize_time(self, value: time) -> str:
        return value.strftime("%H:%M")


class DoseUpdate(Strict):
    recordatorio_id: UUID
    # Hora local programada de la toma, con zona horaria (caso límite 4.1).
    fecha_programada: AwareDatetime
    estado: DoseState
    pospuesto_minutos: int = Field(default=0, ge=0, le=1440)


class DoseOut(BaseModel):
    recordatorio_id: UUID
    fecha_programada: datetime
    estado: str
    pospuesto_minutos: int


def validate_future(value: datetime) -> datetime:
    # Invariante 8: no se programan citas en el pasado.
    if value <= datetime.now(timezone.utc):
        raise ValueError("La cita debe ser en una fecha y hora futuras")
    return value


class AppointmentCreate(Strict):
    especialista: Required255
    especialidad: Annotated[str, StringConstraints(strip_whitespace=True, max_length=100)] = ""
    lugar: Required255
    fecha_hora: Annotated[AwareDatetime, AfterValidator(validate_future)]
    notas: Notes = ""


class AppointmentState(Strict):
    estado: Literal["cancelada"]


class AppointmentOut(BaseModel):
    id: UUID
    especialista: str
    especialidad: str
    lugar: str
    fecha_hora: datetime
    estado: str
    notas: str


def validate_not_future(value: datetime | None) -> datetime | None:
    if value is not None and value > datetime.now(timezone.utc):
        raise ValueError("El registro no puede tener fecha futura")
    return value


class HealthRecordCreate(Strict):
    """Límites de registro del caso límite 4.6, iguales a los que valida la app.

    La presión sistólica llega hasta 250 mmHg porque es el máximo que admite la tabla.
    """

    dolor: int = Field(ge=1, le=10)
    estado_animo: Mood
    presion_sistolica: int = Field(ge=70, le=250)
    presion_diastolica: int = Field(ge=40, le=150)
    glucosa: float = Field(ge=40, le=500)
    peso: float = Field(gt=0, lt=1000)
    pulso: int = Field(ge=40, le=220)
    notas: Notes = ""
    # Momento en que se tomó el registro en el dispositivo; por defecto, ahora.
    fecha_registro: Annotated[AwareDatetime | None, AfterValidator(validate_not_future)] = None

    @model_validator(mode="after")
    def systolic_above_diastolic(self):
        if self.presion_sistolica <= self.presion_diastolica:
            raise ValueError("La presión sistólica debe ser mayor que la diastólica")
        return self


class HealthRecordOut(BaseModel):
    id: UUID
    fecha_registro: datetime
    dolor: int | None
    estado_animo: str | None
    presion_sistolica: int | None
    presion_diastolica: int | None
    glucosa: float | None
    peso: float | None
    pulso: int | None
    notas: str
