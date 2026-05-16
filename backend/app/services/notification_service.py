import logging
from datetime import datetime, timedelta, timezone
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func

from app.models.chat import Notification, Chat, Message, CourseSuggestion
from app.models.swipe import Match
from app.models.task import Task, ProjectEvaluation
from app.models.project import Project, ProjectMember
from app.models.user import User
from app.models.profile import UserProfile, UserRole, UserSkill
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


async def create_notification(
    db: AsyncSession,
    user_id: UUID,
    type: str,
    title: str,
    body: str,
    related_id: str | None = None,
) -> Notification:
    notif = Notification(
        user_id=user_id,
        type=type,
        title=title,
        body=body,
        related_id=related_id,
    )
    db.add(notif)
    return notif


async def notify_task_assigned(db: AsyncSession, task: Task):
    await create_notification(
        db,
        task.assignee_id,
        "TASK_ASSIGNED",
        "New task assigned",
        f"'{task.title}' has been assigned to you",
        str(task.id),
    )


async def notify_deadline_reminder(db: AsyncSession, task: Task):
    await create_notification(
        db,
        task.assignee_id,
        "DEADLINE_REMINDER",
        "Task deadline approaching",
        f"'{task.title}' is due in 2 days",
        str(task.id),
    )
    await create_notification(
        db,
        task.creator_id,
        "DEADLINE_REMINDER_OWNER",
        "Member task deadline approaching",
        f"'{task.title}' assigned to a member is due in 2 days",
        str(task.id),
    )


async def notify_checkout(db: AsyncSession, task: Task, checkout_status: str, assignee_name: str):
    await create_notification(
        db,
        task.creator_id,
        "TASK_CHECKOUT",
        f"{assignee_name} completed: {task.title}",
        f"Status: {checkout_status}. Review within {settings.CHECKOUT_REVIEW_HOURS} hours.",
        str(task.id),
    )


async def notify_member_added(db: AsyncSession, user_id: UUID, project_id: UUID):
    project = await db.get(Project, project_id)
    await create_notification(
        db,
        user_id,
        "ADDED_TO_PROJECT",
        "You've been added to a project!",
        f"Congratulations! You are now a member of '{project.title if project else 'Unknown'}'",
        str(project_id),
    )


async def get_unread_notification_count(db: AsyncSession, user_id: UUID) -> int:
    result = await db.execute(
        select(func.count()).select_from(Notification).where(
            Notification.user_id == user_id,
            Notification.is_read == False,
        )
    )
    return result.scalar() or 0


async def get_notifications(db: AsyncSession, user_id: UUID, limit: int = 20) -> list[Notification]:
    result = await db.execute(
        select(Notification)
        .where(Notification.user_id == user_id)
        .order_by(Notification.created_at.desc())
        .limit(limit)
    )
    return list(result.scalars().all())
