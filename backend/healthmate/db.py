import os
import ssl

from sqlalchemy import URL, make_url
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine


def database_settings(value: str) -> tuple[URL, dict]:
    url = make_url(value)
    if url.drivername not in {"postgres", "postgresql", "postgresql+asyncpg"}:
        raise ValueError("DATABASE_URL debe usar PostgreSQL")
    if not url.host or not url.database:
        raise ValueError("DATABASE_URL requiere servidor y nombre de base de datos")

    sslmode = url.query.get("sslmode", "verify-full")
    url = url.set(drivername="postgresql+asyncpg").difference_update_query(
        ["sslmode", "channel_binding"]
    )
    if sslmode == "disable" and url.host in {"localhost", "127.0.0.1", "::1"}:
        return url, {"ssl": False}
    if sslmode not in {"require", "verify-full"}:
        raise ValueError("Usa sslmode=verify-full para conexiones remotas")

    context = ssl.create_default_context()
    context.minimum_version = ssl.TLSVersion.TLSv1_3
    return url, {"ssl": context}


def make_engine(value: str | None = None) -> AsyncEngine:
    url, options = database_settings(value if value is not None else os.environ["DATABASE_URL"])
    return create_async_engine(
        url, connect_args=options, pool_pre_ping=True, hide_parameters=True
    )
