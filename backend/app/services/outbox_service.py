import logging
from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy import select

from app.database import async_session
from app.models.operations import OutboxEvent
from app.models.feedback import AnonymousFeedback
from app.models.chat import Notification
from app.services.fcm_service import notify_user
from app.services.mistral_service import analyze_feedback_weaknesses

logger = logging.getLogger(__name__)


async def process_outbox() -> None:
    async with async_session() as db:
        events = (
            await db.execute(
                select(OutboxEvent)
                .where(OutboxEvent.processed_at.is_(None), OutboxEvent.attempts < 10)
                .order_by(OutboxEvent.created_at)
                .limit(50)
                .with_for_update(skip_locked=True)
            )
        ).scalars().all()
        for event in events:
            try:
                if event.event_type == "PUSH_NOTIFICATION":
                    payload = event.payload
                    await notify_user(
                        db,
                        UUID(payload["user_id"]),
                        payload["title"],
                        payload["body"],
                        payload.get("data"),
                    )
                elif event.event_type == "FEEDBACK_ANALYSIS":
                    payload = event.payload
                    feedback = await db.get(
                        AnonymousFeedback, UUID(payload["feedback_id"])
                    )
                    if feedback:
                        feedback.analyzed_weaknesses = (
                            await analyze_feedback_weaknesses(
                                feedback.feedback_text
                            )
                        )
                        db.add(
                            Notification(
                                user_id=UUID(payload["target_id"]),
                                type="FEEDBACK_RECEIVED",
                                title="Anonymous feedback received",
                                body=f"You received feedback for {payload['project_title']}",
                                related_id=payload["project_id"],
                            )
                        )
                        db.add(
                            OutboxEvent(
                                event_type="PUSH_NOTIFICATION",
                                payload={
                                    "user_id": payload["target_id"],
                                    "title": "Anonymous feedback",
                                    "body": f"You received feedback for {payload['project_title']}",
                                },
                            )
                        )
                else:
                    raise ValueError(f"Unsupported outbox event type: {event.event_type}")
                event.processed_at = datetime.now(timezone.utc)
                event.last_error = None
            except Exception as exc:
                event.attempts += 1
                event.last_error = str(exc)[:2000]
                logger.exception("Outbox event %s failed", event.id)
        await db.commit()
