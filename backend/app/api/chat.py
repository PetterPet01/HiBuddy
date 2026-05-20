from datetime import datetime
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import get_db
from app.core.dependencies import get_current_user
from app.core.security import decode_token
from app.models.user import User
from app.models.swipe import Match
from app.models.chat import Chat, Message, Notification
from app.models.project import Project
from app.api.websocket import manager
from app.services.notification_service import get_notifications, get_unread_notification_count
from app.services.presence_service import presence_manager

router = APIRouter(prefix="/api/v1", tags=["chat"])


@router.get("/chat/inbox")
async def get_inbox(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    matches_result = await db.execute(
        select(Match).where(
            ((Match.user_id == current_user.id) | (Match.owner_id == current_user.id)) &
            (Match.is_unmatched == False)
        ).order_by(Match.matched_at.desc())
    )
    matches = matches_result.scalars().all()

    chats = []
    for match in matches:
        other_user_id = match.user_id if match.owner_id == current_user.id else match.owner_id
        other_user = await db.get(User, other_user_id)
        project = await db.get(Project, match.project_id)
        presence = presence_manager.get_presence(str(other_user_id))

        chat_result = await db.execute(
            select(Chat).where(Chat.match_id == match.id)
        )
        chat = chat_result.scalar_one_or_none()

        last_msg_result = await db.execute(
            select(Message).where(
                Message.chat_id == chat.id if chat else None
            ).order_by(Message.created_at.desc()).limit(1)
        ) if chat else None

        unread_count = 0
        if chat:
            unread_result = await db.execute(
                select(func.count()).select_from(Message).where(
                    Message.chat_id == chat.id,
                    Message.sender_id != current_user.id,
                    Message.is_read == False,
                )
            )
            unread_count = unread_result.scalar() or 0

        last_msg = last_msg_result.scalar_one_or_none() if last_msg_result else None

        chats.append({
            "id": str(chat.id) if chat else "",
            "match_id": str(match.id),
            "user_id": str(other_user_id),
            "user_name": other_user.full_name if other_user else "",
            "user_avatar": other_user.avatar_url if other_user else None,
            "user_is_online": presence["is_online"],
            "user_last_seen_at": presence["last_seen_at"],
            "project_title": project.title if project else "",
            "last_message": last_msg.content[:100] if last_msg else None,
            "last_message_time": last_msg.created_at.isoformat() if last_msg else None,
            "is_unread": unread_count > 0,
            "unread_count": unread_count,
            "matched_at": match.matched_at.isoformat(),
        })

    return sorted(
        chats,
        key=lambda item: item["last_message_time"] or item["matched_at"],
        reverse=True,
    )


@router.get("/chat/{match_id}/messages")
async def get_messages(
    match_id: UUID,
    limit: int = 50,
    before: str | None = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    match = await db.get(Match, match_id)
    if not match or (match.user_id != current_user.id and match.owner_id != current_user.id):
        raise HTTPException(status_code=403, detail="Not authorized")

    chat_result = await db.execute(select(Chat).where(Chat.match_id == match_id))
    chat = chat_result.scalar_one_or_none()
    if not chat:
        return []

    limit = min(max(limit, 1), 100)
    query = select(Message).where(Message.chat_id == chat.id)
    if before:
        try:
            before_dt = datetime.fromisoformat(before.replace("Z", "+00:00"))
            query = query.where(Message.created_at < before_dt)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid before timestamp")

    query = query.order_by(Message.created_at.desc()).limit(limit)
    messages = (await db.execute(query)).scalars().all()

    unread_result = await db.execute(
        select(Message).where(
            Message.chat_id == chat.id,
            Message.sender_id != current_user.id,
            Message.is_read == False,
        )
    )
    unread_messages = unread_result.scalars().all()
    for msg in unread_messages:
        msg.is_read = True
    if unread_messages:
        await db.commit()
        other_user_id = match.user_id if match.owner_id == current_user.id else match.owner_id
        await manager.send_message(str(other_user_id), str(match_id), {
            "type": "read_receipt",
            "by": str(current_user.id),
        })

    response = []
    for msg in reversed(messages):
        sender = await db.get(User, msg.sender_id)
        response.append({
            "id": str(msg.id),
            "chat_id": str(msg.chat_id),
            "sender_id": str(msg.sender_id),
            "content": msg.content,
            "is_read": msg.is_read,
            "created_at": msg.created_at.isoformat(),
            "sender_name": sender.full_name if sender else None,
        })
    return response


@router.get("/notifications")
async def list_notifications(
    limit: int = 20,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    notifications = await get_notifications(db, current_user.id, limit)
    return [
        {
            "id": str(n.id),
            "type": n.type,
            "title": n.title,
            "body": n.body,
            "is_read": n.is_read,
            "related_id": n.related_id,
            "created_at": n.created_at.isoformat(),
        }
        for n in notifications
    ]


@router.post("/notifications/{notification_id}/read")
async def mark_notification_read(
    notification_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    notif = await db.get(Notification, notification_id)
    if notif and notif.user_id == current_user.id:
        notif.is_read = True
    return {"message": "Notification marked as read"}


@router.get("/notifications/unread-count")
async def unread_count(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    count = await get_unread_notification_count(db, current_user.id)
    return {"count": count}
