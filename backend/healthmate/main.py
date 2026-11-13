import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import async_sessionmaker

from .db import make_engine
from .routers import appointments, auth, health, medications, users


def create_app(
    database_url: str | None = None,
    secret: str | None = None,
    *,
    google_client_id: str | None = None,
) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app: FastAPI):
        key = secret if secret is not None else os.environ["JWT_SECRET"]
        if len(key.encode()) < 32:
            raise ValueError("JWT_SECRET debe tener al menos 32 bytes")
        engine = make_engine(database_url)
        app.state.sessions = async_sessionmaker(engine, expire_on_commit=False)
        app.state.secret = key
        app.state.google_client_id = (
            google_client_id if google_client_id is not None else os.getenv("GOOGLE_CLIENT_ID", "")
        ).strip()
        try:
            yield
        finally:
            await engine.dispose()

    app = FastAPI(title="HealthMate API", lifespan=lifespan)
    app.include_router(auth.router)
    app.include_router(users.router)
    app.include_router(medications.router)
    app.include_router(appointments.router)
    app.include_router(health.router)

    @app.exception_handler(RequestValidationError)
    async def validation_error(_request: Request, exc: RequestValidationError):
        # Los errores de validación por defecto pueden incluir contraseñas o datos médicos.
        fields = [{"loc": error["loc"], "type": error["type"]} for error in exc.errors()]
        return JSONResponse(status_code=422, content={"detail": fields})

    return app


app = create_app()
