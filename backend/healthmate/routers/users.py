from fastapi import APIRouter, Response

from ..dependencies import CurrentUser, Db
from ..schemas import Profile, ProfileUpdate

router = APIRouter(prefix="/usuarios", tags=["usuarios"])


@router.get("/me", response_model=Profile)
async def get_profile(user: CurrentUser, response: Response):
    response.headers["Cache-Control"] = "no-store"
    return user


@router.put("/me", response_model=Profile)
async def update_profile(body: ProfileUpdate, user: CurrentUser, db: Db, response: Response):
    for name, value in body.model_dump().items():
        setattr(user, name, value)
    await db.commit()
    response.headers["Cache-Control"] = "no-store"
    return user
