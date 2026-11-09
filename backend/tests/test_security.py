import ssl
from datetime import date, timedelta

import pytest
from jose import jwt
from pydantic import ValidationError

from healthmate.db import database_settings
from healthmate.models import User
from healthmate.routers.auth import DUMMY_HASH
from healthmate.schemas import ProfileUpdate, Registration
from healthmate.security import TokenError, decode_token, issue_tokens, passwords

KEY = "test-secret-" * 4


def test_password_hash_is_argon2_and_verifies():
    hashed = passwords.hash("a long test password")
    assert hashed.startswith("$argon2id$")
    assert passwords.verify("a long test password", hashed)
    assert not passwords.verify("wrong", hashed)
    assert not passwords.verify("wrong", DUMMY_HASH)


def test_tokens_are_typed_signed_and_expire():
    user = User(
        id=42, nombre="Ana", email="ana@example.com", alergias="",
        condiciones_cronicas="", contacto_emergencia="",
        fecha_nacimiento=date(1992, 5, 14),
    )
    tokens, session = issue_tokens(user, KEY)
    assert decode_token(tokens.access_token, KEY, "access") == (42, None)
    assert decode_token(tokens.refresh_token, KEY, "refresh") == (42, session.id)
    with pytest.raises(TokenError):
        decode_token(tokens.refresh_token, KEY, "access")
    with pytest.raises(TokenError):
        decode_token(tokens.access_token, "another-secret" * 4, "access")
    claims = jwt.get_unverified_claims(tokens.access_token)
    claims["exp"] = 0
    expired = jwt.encode(claims, KEY, algorithm="HS256")
    with pytest.raises(TokenError):
        decode_token(expired, KEY, "access")


def test_registration_normalizes_email_and_rejects_client_role():
    body = {
        "nombre": " Ana ", "email": "ANA@example.com", "password": "a long test password",
        "fecha_nacimiento": "1992-05-14",
    }
    registration = Registration.model_validate(body)
    assert registration.nombre == "Ana"
    assert registration.email == "ana@example.com"
    with pytest.raises(ValidationError):
        Registration.model_validate({**body, "rol": "admin"})


@pytest.mark.parametrize(
    "invalid",
    [
        {"peso": float("nan")}, {"peso": "64.123"}, {"altura": -1},
        {"fecha_nacimiento": date.today() + timedelta(days=2)},
    ],
)
def test_profile_validates_input(invalid):
    with pytest.raises(ValidationError):
        ProfileUpdate.model_validate(
            {"nombre": "Ana", "fecha_nacimiento": "1992-05-14", **invalid}
        )


def test_database_url_enforces_verified_tls_for_remote_hosts():
    url, options = database_settings(
        "postgresql://test:secret@db.example.com/healthmate?sslmode=require&channel_binding=require"
    )
    assert url.drivername == "postgresql+asyncpg"
    assert not url.query
    assert options["ssl"].check_hostname
    assert options["ssl"].minimum_version == ssl.TLSVersion.TLSv1_3
    with pytest.raises(ValueError):
        database_settings("postgresql://db.example.com/healthmate?sslmode=disable")
