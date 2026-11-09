import json
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from uuid import uuid4

import pytest
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from jose import jwt

from healthmate import google_identity

CLIENT_ID = "healthmate-test.apps.googleusercontent.com"


@pytest.fixture
def signed_google_token(monkeypatch):
    now = datetime.now(timezone.utc)
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Test key")])
    certificate = (
        x509.CertificateBuilder().subject_name(name).issuer_name(name)
        .public_key(key.public_key()).serial_number(x509.random_serial_number())
        .not_valid_before(now - timedelta(minutes=1)).not_valid_after(now + timedelta(minutes=5))
        .sign(key, hashes.SHA256()).public_bytes(serialization.Encoding.PEM).decode()
    )
    pem = key.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    cert_response = SimpleNamespace(status=200, data=json.dumps({"test-key": certificate}).encode())
    monkeypatch.setattr(google_identity, "Request", lambda **_: lambda *args, **kw: cert_response)

    def sign(**claims):
        return jwt.encode({
            "sub": "google-identity", "email": "ana@example.com", "name": "Ana",
            "email_verified": True, "nonce": str(uuid4()), "aud": CLIENT_ID,
            "iss": "https://accounts.google.com", "iat": now, "exp": now + timedelta(minutes=5),
            **claims,
        }, pem, algorithm="RS256", headers={"kid": "test-key"})

    return sign


def test_accepts_google_identity_with_verified_email(signed_google_token):
    nonce = uuid4()
    identity = google_identity.verify_google_identity(signed_google_token(nonce=str(nonce)), CLIENT_ID)
    assert identity.sub == "google-identity"
    assert identity.nonce == nonce


@pytest.mark.parametrize("invalid", [
    {"aud": "another-client"}, {"iss": "https://example.com"}, {"exp": 0},
    {"nonce": "invalid"}, {"email_verified": False},
])
def test_rejects_invalid_google_claims(signed_google_token, invalid):
    with pytest.raises(google_identity.InvalidGoogleIdentity):
        google_identity.verify_google_identity(signed_google_token(**invalid), CLIENT_ID)


def test_rejects_altered_signature(signed_google_token):
    header, payload, signature = signed_google_token().split(".")
    altered = ("A" if signature[0] != "A" else "B") + signature[1:]
    with pytest.raises(google_identity.InvalidGoogleIdentity):
        google_identity.verify_google_identity(f"{header}.{payload}.{altered}", CLIENT_ID)
