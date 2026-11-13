from functools import partial
from uuid import UUID

from google.auth.exceptions import GoogleAuthError, TransportError
from google.auth.transport.requests import Request
from google.oauth2 import id_token
from pydantic import BaseModel, EmailStr, Field
from requests import Session


class InvalidGoogleIdentity(ValueError):
    pass


class GoogleUnavailable(RuntimeError):
    pass


class GoogleIdentity(BaseModel):
    sub: str = Field(min_length=1, max_length=255)
    email: EmailStr = Field(max_length=254)
    nonce: UUID
    name: str = ""


def verify_google_identity(token: str, client_id: str) -> GoogleIdentity:
    try:
        with Session() as session:
            transport = partial(Request(session=session), timeout=5)
            claims = id_token.verify_oauth2_token(token, transport, audience=client_id)
        if claims.get("email_verified") is not True:
            raise InvalidGoogleIdentity("Correo de Google sin verificar")
        return GoogleIdentity.model_validate(claims)
    except TransportError as exc:
        raise GoogleUnavailable("No se pudo consultar la identidad de Google") from exc
    except (GoogleAuthError, ValueError) as exc:
        raise InvalidGoogleIdentity("Identidad de Google inválida") from exc
