import asyncio
import os
from datetime import datetime, timedelta, timezone
from uuid import UUID, uuid4

import httpx
import pytest
import pytest_asyncio

from healthmate.main import create_app
from healthmate.google_identity import GoogleIdentity
from sqlalchemy import select, update

from healthmate.models import GoogleChallenge, Perfil, Role, User, UserRole
from healthmate.routers import auth as auth_router
from healthmate.security import passwords

KEY = "test-secret-" * 4
PASSWORD = "a long test password"
BIRTH_DATE = "1992-05-14"
GOOGLE_CLIENT_ID = "healthmate-test.apps.googleusercontent.com"


@pytest_asyncio.fixture
async def client():
    url = os.environ.get("HEALTHMATE_TEST_DATABASE_URL")
    if not url:
        pytest.skip("Configura HEALTHMATE_TEST_DATABASE_URL con una base de pruebas migrada.")
    app = create_app(url, KEY, google_client_id=GOOGLE_CLIENT_ID)
    async with app.router.lifespan_context(app):
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app), base_url="http://test"
        ) as http:
            yield http, app.state.sessions


async def register(http):
    email = f"{uuid4().hex}@example.com"
    response = await http.post(
        "/auth/registro",
        json={"nombre": "Ana", "email": email, "password": PASSWORD, "fecha_nacimiento": BIRTH_DATE},
    )
    assert response.status_code == 201, response.text
    return email, response.json()


def auth(tokens):
    return {"Authorization": "Bearer " + tokens["access_token"]}


@pytest.mark.asyncio
async def test_account_creation_and_login(client):
    http, sessions = client
    email, tokens = await register(http)
    async with sessions() as session:
        user_id = UUID(tokens["usuario"]["id"])
        user = await session.get(User, user_id)
        role = await session.scalar(
            select(Role.nombre).join(UserRole, UserRole.rol_id == Role.id).where(UserRole.usuario_id == user_id)
        )
        assert role == "paciente"
        perfil = await session.scalar(select(Perfil).where(Perfil.usuario_id == user_id))
        assert perfil.nombre_completo == "Ana"
        assert user.password_hash != PASSWORD
        assert passwords.verify(PASSWORD, user.password_hash)
    duplicate = await http.post(
        "/auth/registro",
        json={"nombre": "Ana", "email": email.upper(), "password": PASSWORD, "fecha_nacimiento": BIRTH_DATE},
    )
    assert duplicate.status_code == 409
    wrong = await http.post("/auth/login", json={"email": email, "password": "wrong"})
    unknown = await http.post("/auth/login", json={"email": "missing@example.com", "password": "wrong"})
    assert wrong.status_code == unknown.status_code == 401
    assert wrong.json() == unknown.json()
    login = await http.post("/auth/login", json={"email": email, "password": PASSWORD})
    assert login.status_code == 200
    assert login.headers["cache-control"] == "no-store"


@pytest.mark.asyncio
async def test_profile_update_is_persisted_and_scoped_to_account(client):
    http, _ = client
    _, first = await register(http)
    _, second = await register(http)
    assert (await http.get("/usuarios/me")).status_code == 401
    payload = {"nombre": "Ana López", "peso": 64.5, "fecha_nacimiento": "1992-05-14"}
    response = await http.put("/usuarios/me", json=payload, headers=auth(first))
    assert response.status_code == 200
    profile = await http.get("/usuarios/me", headers=auth(first))
    assert profile.json() == response.json()
    assert profile.json()["nombre"] == payload["nombre"]
    assert profile.json()["fecha_nacimiento"] == payload["fecha_nacimiento"]
    assert profile.json()["peso"] == "64.50"
    assert profile.json()["id"] == first["usuario"]["id"]
    other = await http.get("/usuarios/me", headers=auth(second))
    assert other.json() == second["usuario"]
    invalid = await http.put(
        "/usuarios/me", json={**payload, "id": second["usuario"]["id"]}, headers=auth(first)
    )
    assert invalid.status_code == 422


@pytest.mark.asyncio
async def test_refresh_token_rotates_once_under_concurrent_requests(client):
    http, _ = client
    _, tokens = await register(http)
    payload = {"refresh_token": tokens["refresh_token"]}
    assert (
        await http.get("/usuarios/me", headers={"Authorization": "Bearer " + payload["refresh_token"]})
    ).status_code == 401
    responses = await asyncio.gather(
        http.post("/auth/refresh", json=payload), http.post("/auth/refresh", json=payload)
    )
    assert sorted(response.status_code for response in responses) == [200, 401]
    winner = next(response.json() for response in responses if response.status_code == 200)
    assert winner["refresh_token"] != payload["refresh_token"]
    assert (await http.get("/usuarios/me", headers=auth(winner))).status_code == 200


@pytest.mark.asyncio
async def test_invalid_password_is_not_reflected_in_validation_response(client):
    http, _ = client
    response = await http.post(
        "/auth/registro",
        json={"nombre": "Ana", "email": "ana@example.com", "password": "secret", "fecha_nacimiento": BIRTH_DATE},
    )
    assert response.status_code == 422
    assert "secret" not in response.text


async def google_challenge(http):
    response = await http.post("/auth/google/desafio")
    assert response.status_code == 201
    assert response.headers["cache-control"] == "no-store"
    return UUID(response.json()["nonce"])


@pytest.mark.asyncio
async def test_google_enrollment_preserves_identity_and_rejects_replay(client, monkeypatch):
    http, sessions = client
    nonce = await google_challenge(http)
    google_email = f"{uuid4().hex}@example.com"
    identity = GoogleIdentity(sub=uuid4().hex, email=google_email, name="Ana", nonce=nonce)

    def verify(token, client_id):
        assert token == "test-google-token"
        assert client_id == GOOGLE_CLIENT_ID
        return identity

    monkeypatch.setattr(auth_router, "verify_google_identity", verify)
    payload = {"id_token": "test-google-token"}
    assert (await http.post("/auth/google", json=payload)).status_code == 428
    responses = await asyncio.gather(*[
        http.post("/auth/google", json={**payload, "fecha_nacimiento": BIRTH_DATE})
        for _ in range(2)
    ])
    assert sorted(response.status_code for response in responses) == [200, 401]
    first = next(response.json() for response in responses if response.status_code == 200)
    assert first["usuario"]["fecha_nacimiento"] == BIRTH_DATE
    assert (await http.get("/usuarios/me", headers=auth(first))).status_code == 200

    async with sessions() as session:
        user = await session.get(User, UUID(first["usuario"]["id"]))
        assert not passwords.verify(PASSWORD, user.password_hash)
        assert user.google_id == identity.sub
        assert await session.get(GoogleChallenge, nonce) is None
    response = await http.post("/auth/login", json={"email": google_email, "password": PASSWORD})
    assert response.status_code == 401

    identity = identity.model_copy(update={
        "nonce": await google_challenge(http), "email": "renamed@example.com"
    })
    returning = await http.post("/auth/google", json=payload)
    assert returning.status_code == 200
    assert returning.json()["usuario"] == first["usuario"]


@pytest.mark.asyncio
async def test_google_email_collision_cannot_take_over_an_existing_account(client, monkeypatch):
    http, _ = client
    email, _ = await register(http)
    identity = GoogleIdentity(
        sub=uuid4().hex, email=email, nonce=await google_challenge(http)
    )
    monkeypatch.setattr(auth_router, "verify_google_identity", lambda *_: identity)
    payload = {"id_token": "test-google-token", "fecha_nacimiento": BIRTH_DATE}
    assert (await http.post("/auth/google", json=payload)).status_code == 409
    original_login = await http.post("/auth/login", json={"email": email, "password": PASSWORD})
    assert original_login.status_code == 200


@pytest.mark.parametrize("expired", [False, True])
@pytest.mark.asyncio
async def test_google_login_requires_an_unexpired_server_nonce(client, monkeypatch, expired):
    http, sessions = client
    nonce = uuid4()
    if expired:
        async with sessions() as session:
            session.add(GoogleChallenge(
                nonce=nonce, vence_en=datetime.now(timezone.utc) - timedelta(minutes=1)
            ))
            await session.commit()
    identity = GoogleIdentity(sub=uuid4().hex, email="ana@example.com", nonce=nonce)
    monkeypatch.setattr(auth_router, "verify_google_identity", lambda *_: identity)
    assert (
        await http.post("/auth/google", json={"id_token": "test-google-token"})
    ).status_code == 401


@pytest.mark.asyncio
async def test_allergies_and_conditions_round_trip_through_related_tables(client):
    http, _ = client
    _, tokens = await register(http)
    allergy = f"Alergia {uuid4().hex[:8]}"
    payload = {
        "nombre": "Ana", "fecha_nacimiento": BIRTH_DATE, "altura": 165,
        "alergias": f"{allergy}, Penicilina, penicilina",
        "condiciones_cronicas": "Hipertensión; Diabetes tipo 2",
        "contacto_emergencia": "María - 555-0192",
    }
    saved = await http.put("/usuarios/me", json=payload, headers=auth(tokens))
    assert saved.status_code == 200, saved.text
    body = saved.json()
    assert body["alergias"] == ", ".join(sorted([allergy, "Penicilina"]))
    # Igual que las alergias, se devuelven en orden alfabético.
    assert body["condiciones_cronicas"] == "Diabetes tipo 2, Hipertensión"
    assert body["altura"] == 165
    assert body["contacto_emergencia"] == "María - 555-0192"

    fewer = await http.put(
        "/usuarios/me", json={**payload, "alergias": "", "condiciones_cronicas": "Diabetes tipo 2"},
        headers=auth(tokens),
    )
    assert fewer.json()["alergias"] == ""
    assert fewer.json()["condiciones_cronicas"] == "Diabetes tipo 2"


@pytest.mark.asyncio
async def test_disabled_account_cannot_log_in_or_use_tokens(client):
    http, sessions = client
    email, tokens = await register(http)
    async with sessions() as session:
        await session.execute(update(User).where(User.email == email).values(estado=False))
        await session.commit()
    assert (await http.post("/auth/login", json={"email": email, "password": PASSWORD})).status_code == 401
    assert (await http.get("/usuarios/me", headers=auth(tokens))).status_code == 401
    assert (await http.post("/auth/refresh", json={"refresh_token": tokens["refresh_token"]})).status_code == 401
