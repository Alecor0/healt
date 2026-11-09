from fastapi import APIRouter, Response

from ..dependencies import CurrentUser, Db
from ..profiles import load_profile, save_profile
from ..schemas import Profile, ProfileUpdate

router = APIRouter(prefix="/usuarios", tags=["usuarios"])


@router.get("/me", response_model=Profile)
async def get_profile(user: CurrentUser, db: Db, response: Response):
    profile = await load_profile(db, user)
    await db.commit()  # por si hubo que crear el perfil vacío
    response.headers["Cache-Control"] = "no-store"
    return profile


@router.put("/me", response_model=Profile)
async def update_profile(body: ProfileUpdate, user: CurrentUser, db: Db, response: Response):
    await save_profile(db, user, body)
    await db.flush()
    profile = await load_profile(db, user)
    await db.commit()
    response.headers["Cache-Control"] = "no-store"
    return profile
