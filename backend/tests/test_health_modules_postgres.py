from datetime import datetime, timedelta, timezone
from uuid import UUID, uuid4

import pytest
from sqlalchemy import select

from healthmate.models import Dose, UserMedication
from test_accounts_postgres import auth, client, register  # noqa: F401  (client es un fixture)

MEDICATION = {
    "nombre": "Paracetamol",
    "dosis": "500mg",
    "frecuencia": "Cada 8 horas",
    "via_administracion": "Oral",
    "hora": "08:00",
    "fecha_inicio": "2026-10-01",
    "proposito": "Dolor leve",
}
RECORD = {
    "dolor": 3,
    "estado_animo": "feliz",
    "presion_sistolica": 120,
    "presion_diastolica": 80,
    "glucosa": 95.5,
    "peso": 68.5,
    "pulso": 78,
    "notas": "Sin novedades",
}


def future(hours=48):
    return (datetime.now(timezone.utc) + timedelta(hours=hours)).isoformat()


async def new_user(http):
    _, tokens = await register(http)
    return auth(tokens)


@pytest.mark.asyncio
async def test_medication_lifecycle_keeps_dose_history(client):
    http, sessions = client
    headers = await new_user(http)

    created = await http.post("/medicamentos", json=MEDICATION, headers=headers)
    assert created.status_code == 201, created.text
    medication = created.json()
    assert medication["hora"] == "08:00"
    assert medication["activo"] is True
    assert medication["fecha_fin"] is None
    assert created.headers["cache-control"] == "no-store"

    listed = await http.get("/medicamentos", headers=headers)
    assert [m["id"] for m in listed.json()] == [medication["id"]]

    paused = await http.patch(f"/medicamentos/{medication['id']}", json={"activo": False}, headers=headers)
    assert paused.status_code == 200
    assert paused.json()["activo"] is False

    scheduled = "2026-10-13T08:00:00-06:00"
    dose = await http.put(
        "/medicamentos/dosis",
        json={"recordatorio_id": medication["recordatorio_id"], "fecha_programada": scheduled, "estado": "tomado"},
        headers=headers,
    )
    assert dose.status_code == 200, dose.text

    deleted = await http.delete(f"/medicamentos/{medication['id']}", headers=headers)
    assert deleted.status_code == 204
    assert (await http.get("/medicamentos", headers=headers)).json() == []
    again = await http.delete(f"/medicamentos/{medication['id']}", headers=headers)
    assert again.status_code == 404

    # Caso límite 4.5: el borrado es lógico y el historial de dosis se conserva.
    async with sessions() as session:
        row = await session.get(UserMedication, UUID(medication["id"]))
        assert row.eliminado_en is not None
        history = await session.scalars(
            select(Dose).where(Dose.recordatorio_id == UUID(medication["recordatorio_id"]))
        )
        assert [d.estado for d in history] == ["tomado"]


@pytest.mark.asyncio
async def test_medication_validation(client):
    http, _ = client
    headers = await new_user(http)
    # Invariante 4: fecha_fin >= fecha_inicio.
    bad_dates = await http.post(
        "/medicamentos", json={**MEDICATION, "fecha_fin": "2026-09-30"}, headers=headers
    )
    assert bad_dates.status_code == 422
    same_day = await http.post(
        "/medicamentos", json={**MEDICATION, "fecha_fin": "2026-10-01"}, headers=headers
    )
    assert same_day.status_code == 201
    for invalid in ({"nombre": "  "}, {"hora": "25:00"}, {"dosis": ""}, {"desconocido": 1}):
        response = await http.post("/medicamentos", json={**MEDICATION, **invalid}, headers=headers)
        assert response.status_code == 422, invalid
    assert (await http.get("/medicamentos")).status_code == 401


@pytest.mark.asyncio
async def test_dose_states_and_immutability(client):
    http, _ = client
    headers = await new_user(http)
    medication = (await http.post("/medicamentos", json=MEDICATION, headers=headers)).json()
    scheduled = "2026-10-13T08:00:00-06:00"
    body = {"recordatorio_id": medication["recordatorio_id"], "fecha_programada": scheduled}

    snoozed = await http.put(
        "/medicamentos/dosis", json={**body, "estado": "pospuesto", "pospuesto_minutos": 15}, headers=headers
    )
    assert snoozed.status_code == 200, snoozed.text
    assert snoozed.json()["estado"] == "pospuesto"
    assert snoozed.json()["pospuesto_minutos"] == 15

    taken = await http.put("/medicamentos/dosis", json={**body, "estado": "tomado"}, headers=headers)
    assert taken.status_code == 200
    assert taken.json()["estado"] == "tomado"

    # Invariante 7: una dosis tomada u omitida no se modifica.
    for estado in ("omitido", "pospuesto", "tomado"):
        changed = await http.put("/medicamentos/dosis", json={**body, "estado": estado}, headers=headers)
        assert changed.status_code == 409

    # Invariante 6: solo los estados canónicos.
    invalid = await http.put("/medicamentos/dosis", json={**body, "estado": "pendiente"}, headers=headers)
    assert invalid.status_code == 422
    naive = await http.put(
        "/medicamentos/dosis",
        json={**body, "fecha_programada": "2026-10-14T08:00:00", "estado": "tomado"},
        headers=headers,
    )
    assert naive.status_code == 422

    omitted = await http.put(
        "/medicamentos/dosis",
        json={**body, "fecha_programada": "2026-10-14T08:00:00-06:00", "estado": "omitido"},
        headers=headers,
    )
    assert omitted.json()["estado"] == "omitido"

    day = await http.get(
        "/medicamentos/dosis",
        params={"desde": "2026-10-13T06:00:00Z", "hasta": "2026-10-14T06:00:00Z"},
        headers=headers,
    )
    assert day.status_code == 200, day.text
    assert [d["estado"] for d in day.json()] == ["tomado"]
    assert (await http.get("/medicamentos/dosis", headers=headers)).status_code == 422


@pytest.mark.asyncio
async def test_appointments(client):
    http, _ = client
    headers = await new_user(http)
    body = {
        "especialista": "Dra. García",
        "especialidad": "Cardiología",
        "lugar": "Centro Médico Norte",
        "fecha_hora": future(),
        "notas": "Revisión",
    }
    created = await http.post("/citas", json=body, headers=headers)
    assert created.status_code == 201, created.text
    appointment = created.json()
    assert appointment["estado"] == "confirmada"
    assert appointment["especialidad"] == "Cardiología"
    assert appointment["notas"] == "Revisión"

    # Invariante 8: no se programan citas en el pasado.
    past = await http.post("/citas", json={**body, "fecha_hora": future(-1)}, headers=headers)
    assert past.status_code == 422
    # Invariante 9: fecha, especialista y lugar son obligatorios.
    for missing in ("fecha_hora", "especialista", "lugar"):
        incomplete = {k: v for k, v in body.items() if k != missing}
        assert (await http.post("/citas", json=incomplete, headers=headers)).status_code == 422
    assert (await http.post("/citas", json={**body, "lugar": " "}, headers=headers)).status_code == 422

    cancelled = await http.patch(f"/citas/{appointment['id']}", json={"estado": "cancelada"}, headers=headers)
    assert cancelled.status_code == 200
    assert cancelled.json()["estado"] == "cancelada"
    other_state = await http.patch(
        f"/citas/{appointment['id']}", json={"estado": "completada"}, headers=headers
    )
    assert other_state.status_code == 422
    listed = (await http.get("/citas", headers=headers)).json()
    assert [(a["id"], a["estado"]) for a in listed] == [(appointment["id"], "cancelada")]


@pytest.mark.asyncio
async def test_health_records_and_physiological_limits(client):
    http, _ = client
    headers = await new_user(http)
    first = await http.post(
        "/salud/registros",
        json={**RECORD, "fecha_registro": (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()},
        headers=headers,
    )
    assert first.status_code == 201, first.text
    second = await http.post("/salud/registros", json={**RECORD, "dolor": 5}, headers=headers)
    assert second.status_code == 201
    saved = second.json()
    assert saved["glucosa"] == 95.5 and saved["peso"] == 68.5 and saved["pulso"] == 78

    listed = (await http.get("/salud/registros", headers=headers)).json()
    assert [r["dolor"] for r in listed] == [5, 3]  # el más reciente primero

    # Caso límite 4.6: valores fuera de rango devuelven 422.
    invalid_values = (
        {"presion_sistolica": 1200},
        {"presion_sistolica": 251},
        {"presion_sistolica": 69},
        {"presion_diastolica": 151},
        {"presion_sistolica": 80, "presion_diastolica": 80},
        {"pulso": 0},
        {"pulso": 221},
        {"glucosa": -10},
        {"glucosa": 501},
        {"dolor": 15},
        {"dolor": 0},
        {"peso": 0},
        {"estado_animo": "eufórico"},
        {"fecha_registro": future(1)},
    )
    for invalid in invalid_values:
        response = await http.post("/salud/registros", json={**RECORD, **invalid}, headers=headers)
        assert response.status_code == 422, invalid
    limits = {"presion_sistolica": 250, "presion_diastolica": 150, "pulso": 220, "glucosa": 500, "dolor": 10}
    assert (await http.post("/salud/registros", json={**RECORD, **limits}, headers=headers)).status_code == 201


@pytest.mark.asyncio
async def test_users_never_see_each_others_health_data(client):
    """Invariante 3: aislamiento de datos médicos entre usuarios."""
    http, _ = client
    owner = await new_user(http)
    intruder = await new_user(http)
    medication = (await http.post("/medicamentos", json=MEDICATION, headers=owner)).json()
    appointment = (
        await http.post(
            "/citas",
            json={"especialista": "Dr. Ruiz", "lugar": "Clínica", "fecha_hora": future()},
            headers=owner,
        )
    ).json()
    await http.post("/salud/registros", json=RECORD, headers=owner)
    scheduled = "2026-10-13T08:00:00-06:00"
    await http.put(
        "/medicamentos/dosis",
        json={"recordatorio_id": medication["recordatorio_id"], "fecha_programada": scheduled, "estado": "omitido"},
        headers=owner,
    )

    assert (await http.get("/medicamentos", headers=intruder)).json() == []
    assert (await http.get("/citas", headers=intruder)).json() == []
    assert (await http.get("/salud/registros", headers=intruder)).json() == []
    doses = await http.get(
        "/medicamentos/dosis",
        params={"desde": "2026-10-01T00:00:00Z", "hasta": "2026-11-01T00:00:00Z"},
        headers=intruder,
    )
    assert doses.json() == []

    attempts = (
        await http.patch(f"/medicamentos/{medication['id']}", json={"activo": False}, headers=intruder),
        await http.delete(f"/medicamentos/{medication['id']}", headers=intruder),
        await http.patch(f"/citas/{appointment['id']}", json={"estado": "cancelada"}, headers=intruder),
        await http.put(
            "/medicamentos/dosis",
            json={
                "recordatorio_id": medication["recordatorio_id"],
                "fecha_programada": "2026-10-15T08:00:00-06:00",
                "estado": "tomado",
            },
            headers=intruder,
        ),
        await http.delete(f"/medicamentos/{uuid4()}", headers=intruder),
    )
    assert [r.status_code for r in attempts] == [404] * 5
    assert (await http.get("/medicamentos", headers=owner)).json()[0]["activo"] is True
