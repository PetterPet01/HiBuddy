import logging
from datetime import datetime, timedelta, timezone

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import async_session
from app.models.task import Task, TaskCheckoutHistory
from app.models.project import Project
from app.models.user import User
from app.models.profile import UserProfile
from app.services.notification_service import notify_deadline_reminder, create_notification
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


async def check_deadline_reminders():
    """Run every hour. Find tasks due in 2 days, send reminders."""
    async with async_session() as db:
        try:
            two_days = datetime.now(timezone.utc).date() + timedelta(days=2)
            tasks_result = await db.execute(
                select(Task).where(
                    Task.status.in_(["TODO", "IN_PROGRESS"]),
                    func.date(Task.deadline) == two_days,
                    Task.reminder_sent == False,
                )
            )
            for task in tasks_result.scalars():
                await notify_deadline_reminder(db, task)
                task.reminder_sent = True
            await db.commit()
        except Exception as e:
            logger.error(f"Deadline reminder check failed: {e}")
            await db.rollback()


async def expire_unchecked_tasks():
    """Auto-mark tasks past deadline + grace period as LATE."""
    async with async_session() as db:
        try:
            grace_cutoff = datetime.now(timezone.utc) - timedelta(hours=settings.GRACE_PERIOD_HOURS)
            tasks_result = await db.execute(
                select(Task).where(
                    Task.status.in_(["TODO", "IN_PROGRESS"]),
                    Task.deadline < datetime.now(timezone.utc),
                    Task.deadline <= grace_cutoff,
                )
            )
            for task in tasks_result.scalars():
                previous_status = task.status
                task.checkout_status = "LATE_CHECKOUT"
                task.status = "CLOSED"

                history = TaskCheckoutHistory(
                    task_id=task.id,
                    action="EXPIRE",
                    actor_id=task.assignee_id,
                    previous_status=previous_status,
                    new_status="CLOSED",
                    notes="Auto-expired: deadline passed without checkout",
                )
                db.add(history)

                assignee = await db.get(User, task.assignee_id)
                assignee_name = assignee.full_name if assignee else "Member"

                await create_notification(
                    db, task.assignee_id, "TASK_EXPIRED",
                    "Task marked as late",
                    f"'{task.title}' was auto-marked as late because the deadline passed.",
                    str(task.id),
                )
                await create_notification(
                    db, task.creator_id, "TASK_EXPIRED_OWNER",
                    f"Task for {assignee_name} expired",
                    f"'{task.title}' was auto-marked as late.",
                    str(task.id),
                )

                await _recalculate_user_score(db, task.assignee_id)

            await db.commit()
        except Exception as e:
            logger.error(f"Task expiration check failed: {e}")
            await db.rollback()


async def auto_confirm_checkouts():
    """Confirm checkouts past 72-hour review window."""
    async with async_session() as db:
        try:
            cutoff = datetime.now(timezone.utc) - timedelta(hours=settings.CHECKOUT_REVIEW_HOURS)
            tasks_result = await db.execute(
                select(Task).where(
                    Task.status == "DONE_REVIEW",
                    Task.checkout_confirmed_at == None,
                    Task.checkout_at <= cutoff,
                )
            )
            for task in tasks_result.scalars():
                task.status = "CLOSED"
                task.checkout_confirmed_at = datetime.now(timezone.utc)

                history = TaskCheckoutHistory(
                    task_id=task.id,
                    action="AUTO_CONFIRM",
                    actor_id=task.creator_id,
                    previous_status="DONE_REVIEW",
                    new_status="CLOSED",
                    notes="Auto-confirmed: 72-hour review window expired",
                )
                db.add(history)

                await _recalculate_user_score(db, task.assignee_id)

            await db.commit()
        except Exception as e:
            logger.error(f"Auto-confirm check failed: {e}")
            await db.rollback()


async def _recalculate_user_score(db: AsyncSession, user_id):
    """Recalculate reputation score from closed assigned tasks only.

    Raw per-task values:
    EARLY = 6, ON_TIME = 5, LATE = 3, LATE_CHECKOUT/NOT_COMPLETED = 0.
    The final average is capped at 5.
    """
    tasks_result = await db.execute(
        select(Task).where(
            Task.assignee_id == user_id,
            Task.checkout_status != None,
            Task.status == "CLOSED",
        )
    )
    tasks = tasks_result.scalars().all()

    total_tasks = len(tasks)
    if total_tasks == 0:
        return

    task_scores: list[float] = []
    for task in tasks:
        if task.checkout_status == "EARLY":
            task_scores.append(6.0)
        elif task.checkout_status == "ON_TIME":
            task_scores.append(5.0)
        elif task.checkout_status == "LATE":
            task_scores.append(3.0)
        elif task.checkout_status in ("LATE_CHECKOUT", "NOT_COMPLETED"):
            task_scores.append(0.0)
        else:
            task_scores.append(0.0)

    final_score = sum(task_scores) / len(task_scores)
    final_score = round(max(0, min(5, final_score)), 1)

    profile_result = await db.execute(
        select(UserProfile).where(UserProfile.user_id == user_id)
    )
    profile = profile_result.scalar_one_or_none()
    if profile:
        profile.reputation_score = final_score
