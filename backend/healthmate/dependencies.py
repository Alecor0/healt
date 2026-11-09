from typing import Annotated

from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from .models import User
from .security import TokenError, decode_token


async def get_db(request: Request):
    async with request.app.state.sessions() as session:
        yield session


def get_secret(request: Request) -> str:
    return request.app.state.secret


Db = Annotated[AsyncSession, Depends(get_db)]
Secret = Annotated[str, Depends(get_secret)]
bearer = HTTPBearer(auto_error=False)


def unauthorized() -> HTTPException:
    return HTTPException(
        401, "Credenciales inválidas", headers={"WWW-Authenticate": "Bearer"}
    )


async def get_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
    db: Db,
    secret: Secret,
) -> User:
    if credentials is None:
        raise unauthorized()
    try:
        user_id, _ = decode_token(credentials.credentials, secret, "access")
    except TokenError as exc:
        raise unauthorized() from exc
    user = await db.get(User, user_id)
    if user is None or not user.estado:
        raise unauthorized()
    return user


CurrentUser = Annotated[User, Depends(get_user)]
