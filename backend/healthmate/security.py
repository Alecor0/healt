from datetime import datetime, timedelta, timezone
from typing import Literal
from uuid import UUID, uuid4

from jose import JWTError, jwt
from passlib.context import CryptContext

from .models import RefreshSession
from .schemas import Profile, Tokens

passwords = CryptContext(
    schemes=["argon2"],
    argon2__memory_cost=19456,
    argon2__time_cost=2,
    argon2__parallelism=1,
)
ISSUER = "healthmate"
TokenType = Literal["access", "refresh"]


class TokenError(ValueError):
    pass


def issue_tokens(profile: Profile, secret: str) -> tuple[Tokens, RefreshSession]:
    now = datetime.now(timezone.utc)
    session = RefreshSession(
        id=uuid4(), usuario_id=profile.id, vence_en=now + timedelta(days=7)
    )

    def encode(kind: TokenType, expires: datetime, **claims) -> str:
        return jwt.encode(
            {"sub": str(profile.id), "iss": ISSUER, "iat": now,
             "exp": expires, "tipo": kind, **claims},
            secret,
            algorithm="HS256",
        )

    return Tokens(
        access_token=encode("access", now + timedelta(minutes=15)),
        refresh_token=encode("refresh", session.vence_en, jti=str(session.id)),
        usuario=profile,
    ), session


def decode_token(token: str, secret: str, kind: TokenType) -> tuple[UUID, UUID | None]:
    try:
        payload = jwt.decode(
            token, secret, algorithms=["HS256"], issuer=ISSUER,
            options={"require_exp": True, "require_iat": True, "require_sub": True},
        )
        user_id = UUID(payload["sub"])
        if payload.get("tipo") != kind:
            raise TokenError("Token inválido")
        session_id = UUID(payload["jti"]) if kind == "refresh" else None
        return user_id, session_id
    except (JWTError, KeyError, ValueError, TypeError) as exc:
        raise TokenError("Token inválido") from exc
