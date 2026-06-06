import logging
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.models.feedback import AnonymousFeedback
from app.schemas.feedback import FeedbackCreate, FeedbackResponse, MyFeedbackSummary
from app.models.operations import OutboxEvent

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["feedback"])


@router.post(
    "/projects/{project_id}/feedback/{member_id}",
    response_model=FeedbackResponse,
    status_code=status.HTTP_201_CREATED,
)
async def submit_feedback(
    project_id: UUID,
    member_id: UUID,
    data: FeedbackCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id, options=[selectinload(Project.members)])
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")

    author_id = current_user.id
    target_id = member_id
    if author_id == target_id:
        raise HTTPException(status_code=400, detail="Cannot give feedback to yourself")

    member_ids = {m.user_id for m in project.members}
    if author_id not in member_ids:
        raise HTTPException(status_code=403, detail="You are not a member")
    if target_id not in member_ids:
        raise HTTPException(status_code=404, detail="User is not a member")

    existing = await db.execute(
        select(AnonymousFeedback).where(
            AnonymousFeedback.project_id == project_id,
            AnonymousFeedback.author_id == author_id,
            AnonymousFeedback.target_id == target_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="Already gave feedback to this member")

    feedback = AnonymousFeedback(
        project_id=project_id,
        author_id=author_id,
        target_id=target_id,
        feedback_text=data.feedback_text,
    )
    db.add(feedback)
    await db.flush()

    db.add(
        OutboxEvent(
            event_type="FEEDBACK_ANALYSIS",
            payload={
                "feedback_id": str(feedback.id),
                "project_id": str(project_id),
                "target_id": str(target_id),
                "project_title": project.title,
            },
        )
    )

    return FeedbackResponse.model_validate(feedback)


@router.get("/projects/{project_id}/my-feedback", response_model=MyFeedbackSummary)
async def get_my_feedback(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(AnonymousFeedback).where(
            AnonymousFeedback.project_id == project_id,
            AnonymousFeedback.target_id == current_user.id,
        ).order_by(AnonymousFeedback.created_at.desc())
    )
    feedbacks = result.scalars().all()
    all_weaknesses: list[str] = []
    seen: set[str] = set()
    for fb in feedbacks:
        if fb.analyzed_weaknesses:
            for w in fb.analyzed_weaknesses:
                if w not in seen:
                    seen.add(w)
                    all_weaknesses.append(w)

    return MyFeedbackSummary(
        total_feedbacks=len(feedbacks),
        weaknesses=all_weaknesses,
        feedbacks=[FeedbackResponse.model_validate(fb) for fb in feedbacks],
    )


@router.get("/my-feedback-summary", response_model=MyFeedbackSummary)
async def get_all_my_feedback(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(AnonymousFeedback).where(
            AnonymousFeedback.target_id == current_user.id,
        ).order_by(AnonymousFeedback.created_at.desc())
    )
    feedbacks = result.scalars().all()
    all_weaknesses: list[str] = []
    seen: set[str] = set()
    for fb in feedbacks:
        if fb.analyzed_weaknesses:
            for w in fb.analyzed_weaknesses:
                if w not in seen:
                    seen.add(w)
                    all_weaknesses.append(w)

    return MyFeedbackSummary(
        total_feedbacks=len(feedbacks),
        weaknesses=all_weaknesses,
        feedbacks=[FeedbackResponse.model_validate(fb) for fb in feedbacks],
    )


@router.get("/projects/{project_id}/members-to-feedback")
async def get_members_to_feedback(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id, options=[selectinload(Project.members)])
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    if current_user.id not in {m.user_id for m in project.members}:
        raise HTTPException(status_code=403, detail="Not a member")

    already = await db.execute(
        select(AnonymousFeedback.target_id).where(
            AnonymousFeedback.project_id == project_id,
            AnonymousFeedback.author_id == current_user.id,
        )
    )
    already_ids = {r[0] for r in already.all()}

    members = []
    for m in project.members:
        if m.user_id == current_user.id:
            continue
        user = await db.get(User, m.user_id)
        if not user:
            continue
        members.append({
            "user_id": str(m.user_id),
            "display_name": user.full_name or "",
            "avatar_url": user.avatar_url,
            "role": m.role,
            "already_feedback": m.user_id in already_ids,
        })
    return {"project_id": str(project_id), "project_title": project.title, "members": members}
