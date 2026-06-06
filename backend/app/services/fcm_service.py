import logging
import asyncio
from typing import Any
from uuid import UUID
import firebase_admin
from firebase_admin import credentials, messaging
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.config import get_settings
from app.models.fcm_token import FCMToken

logger = logging.getLogger(__name__)
settings = get_settings()

_initialized = False

def init_firebase():
    global _initialized
    if _initialized:
        return
    try:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _initialized = True
        logger.info("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        logger.error(f"Failed to initialize Firebase Admin SDK: {e}")

async def send_push_notification(
    token: str,
    title: str,
    body: str,
    data: dict[str, str] | None = None
) -> bool:
    if not _initialized:
        init_firebase()

    if not _initialized:
        logger.warning("Firebase not initialized. Cannot send push.")
        return False

    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data or {},
            token=token,
        )
        response = await asyncio.to_thread(messaging.send, message)
        logger.info(f"Successfully sent message: {response}")
        return True
    except Exception as e:
        logger.error(f"Error sending push notification: {e}")
        return False

async def notify_user(
    db: AsyncSession,
    user_id: UUID,
    title: str,
    body: str,
    data: dict[str, str] | None = None
):
    stmt = select(FCMToken).where(FCMToken.user_id == user_id, FCMToken.is_active == True)
    tokens = (await db.execute(stmt)).scalars().all()
    for token_obj in tokens:
        await send_push_notification(token_obj.token, title, body, data)
