from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.profile import UserProfile
from app.schemas.swipe import SwipeActionRequest, DiscoverResponse
from app.services.swipe_service import (
    perform_swipe_action, get_discover_cards, get_matches,
    unmatch, get_applicants_for_project,
)

router = APIRouter(prefix="/api/v1/swipe", tags=["swipe"])


@router.get("/discover")
async def discover_cards(
    mode: str = "CONTRIBUTOR",
    cursor: str | None = None,
    limit: int = 20,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_discover_cards(db, current_user, mode, cursor, limit)


@router.post("/action")
async def swipe_action(
    data: SwipeActionRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        return await perform_swipe_action(db, current_user, data.target_type, data.target_id, data.action)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/matches")
async def list_matches(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_matches(db, current_user)


@router.post("/matches/{match_id}/unmatch")
async def unmatch_action(
    match_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        return await unmatch(db, current_user, match_id)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/applicants/{project_id}")
async def list_applicants(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        return await get_applicants_for_project(db, current_user, project_id)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/stats")
async def swipe_stats(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services.swipe_service import get_daily_likes_remaining, get_daily_superlikes_remaining
    return {
        "daily_likes_remaining": await get_daily_likes_remaining(db, current_user.id),
        "daily_superlikes_remaining": await get_daily_superlikes_remaining(db, current_user.id),
    }
