from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.services.suggestion_service import (
    generate_course_suggestions, suggest_mentors,
    dismiss_course_suggestion, refresh_suggestions,
)

router = APIRouter(prefix="/api/v1/suggestions", tags=["suggestions"])


@router.get("/courses")
async def get_course_suggestions(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await generate_course_suggestions(db, current_user.id)


@router.post("/courses/{suggestion_id}/dismiss")
async def dismiss_course(
    suggestion_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        return await dismiss_course_suggestion(db, current_user.id, suggestion_id)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/refresh")
async def refresh_suggestions_endpoint(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await refresh_suggestions(db, current_user.id)


@router.get("/mentors")
async def get_mentor_suggestions(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await suggest_mentors(db, current_user.id)
