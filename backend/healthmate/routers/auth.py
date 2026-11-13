import secrets
from datetime import datetime, timedelta, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, Request, Response
from sqlalchemy import delete, func, select
from starlette.concurrency import run_in_threadpool

from ..dependencies import Db, Secret, unauthorized
from ..google_identity import GoogleUnavailable, InvalidGoogleIdentity, verify_google_identity
from ..models import GoogleChallenge, RefreshSession, User
from ..profiles import create_account, load_profile
from ..schemas import GoogleChallengeResponse, GoogleLogin, Login, RefreshRequest, Registration, Tokens
from ..security import TokenError, decode_token, issue_tokens, passwords

router = APIRouter(prefix="/auth", tags=["autenticación"])
# Coste de verificación equivalente cuando la cuenta no existe.
DUMMY_HASH = "$argon2id$v=19$m=19456,t=2,p=1$RCiFcI5xrtX6v9d6DyEkxA$5YhInzZXPC2MgycGbNrPnJEZqMq5gHsJeKCMhvxo9WY"


async def start_session(user: User, db: Db, secret: Secret, response: Response) -> Tokens:
    tokens, session = issue_tokens(await load_profile(db, user), secret)
    db.add(session)
    await db.commit()
    response.headers["Cache-Control"] = "no-store"
    return tokens


@router.post("/registro", status_code=201)
async def register(body: Registration, response: Response, db: Db, secret: Secret) -> Tokens:
    password_hash = await run_in_threadpool(passwords.hash, body.password.get_secret_value())
    user = await create_account(
        db, email=body.email, password_hash=password_hash,
        nombre=body.nombre, fecha_nacimiento=body.fecha_nacimiento,
    )
    if user is None:
        raise HTTPException(409, "El correo ya está registrado")
    return await start_session(user, db, secret, response)


@router.post("/login")
async def login(body: Login, response: Response, db: Db, secret: Secret) -> Tokens:
    user = await db.scalar(select(User).where(User.email == body.email))
    valid = await run_in_threadpool(
        passwords.verify, body.password.get_secret_value(), user.password_hash if user else DUMMY_HASH
    )
    if user is None or not valid or not user.estado:
        raise unauthorized()
    return await start_session(user, db, secret, response)


def require_google_client_id(request: Request) -> str:
    client_id = request.app.state.google_client_id
    if not client_id:
        raise HTTPException(503, "El acceso con Google aún no está configurado")
    return client_id


@router.post("/google/desafio", status_code=201)
async def create_google_challenge(
    request: Request, response: Response, db: Db
) -> GoogleChallengeResponse:
    require_google_client_id(request)
    await db.execute(delete(GoogleChallenge).where(GoogleChallenge.vence_en <= func.now()))
    nonce = uuid4()
    db.add(GoogleChallenge(nonce=nonce, vence_en=datetime.now(timezone.utc) + timedelta(minutes=5)))
    await db.commit()
    response.headers["Cache-Control"] = "no-store"
    return GoogleChallengeResponse(nonce=nonce)


@router.post("/google")
async def login_google(
    body: GoogleLogin, request: Request, response: Response, db: Db, secret: Secret
) -> Tokens:
    client_id = require_google_client_id(request)
    try:
        identity = await run_in_threadpool(
            verify_google_identity, body.id_token.get_secret_value(), client_id
        )
    except InvalidGoogleIdentity as exc:
        raise unauthorized() from exc
    except GoogleUnavailable as exc:
        raise HTTPException(503, "Google no está disponible en este momento") from exc

    active_challenge = (
        GoogleChallenge.nonce == identity.nonce,
        GoogleChallenge.vence_en > func.now(),
    )
    user = await db.scalar(select(User).where(User.google_id == identity.sub))
    email = identity.email.lower()
    if user is None and body.fecha_nacimiento is None:
        if await db.scalar(select(GoogleChallenge.nonce).where(*active_challenge)) is None:
            raise unauthorized()
        if await db.scalar(select(User.id).where(User.email == email)) is not None:
            raise HTTPException(409, "Accede con el método original de la cuenta")
        raise HTTPException(428, "Completa la fecha de nacimiento para crear tu cuenta")

    used_nonce = await db.scalar(
        delete(GoogleChallenge).where(*active_challenge).returning(GoogleChallenge.nonce)
    )
    if used_nonce is None:
        raise unauthorized()

    if user is None:
        name = identity.name.strip() or email.split("@", 1)[0]
        # El esquema exige contraseña: se guarda el hash de un valor aleatorio que nadie conoce.
        unusable = await run_in_threadpool(passwords.hash, secrets.token_urlsafe(32))
        user = await create_account(
            db, email=email, password_hash=unusable, nombre=name[:100],
            fecha_nacimiento=body.fecha_nacimiento, google_id=identity.sub,
        )
        if user is None:
            user = await db.scalar(select(User).where(User.google_id == identity.sub))
        if user is None:
            raise HTTPException(409, "Accede con el método original de la cuenta")
    if not user.estado:
        raise unauthorized()
    return await start_session(user, db, secret, response)


@router.post("/refresh")
async def refresh(body: RefreshRequest, response: Response, db: Db, secret: Secret) -> Tokens:
    try:
        user_id, session_id = decode_token(body.refresh_token.get_secret_value(), secret, "refresh")
    except TokenError as exc:
        raise unauthorized() from exc
    user = await db.get(User, user_id, with_for_update={"read": True, "key_share": True})
    if user is None or not user.estado:
        raise unauthorized()
    consumed = await db.scalar(
        delete(RefreshSession)
        .where(
            RefreshSession.id == session_id,
            RefreshSession.usuario_id == user_id,
            RefreshSession.vence_en > func.now(),
        )
        .returning(RefreshSession.id)
    )
    if consumed is None:
        raise unauthorized()
    return await start_session(user, db, secret, response)
