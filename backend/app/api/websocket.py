import json
import logging
from collections import defaultdict
from uuid import UUID

from fastapi import WebSocket, WebSocketDisconnect, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.security import decode_token
from app.database import async_session
from app.models.user import User
from app.models.swipe import Match
from app.models.chat import Chat, Message
from app.services.presence_service import presence_manager
from app.services.fcm_service import notify_user
from app.models.trust_safety import UserBlock
from app.models.operations import OutboxEvent

logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(self):
        self.active_connections: dict[str, dict[str, WebSocket]] = defaultdict(dict)

    async def connect(self, websocket: WebSocket, user_id: str, chat_id: str):
        await websocket.accept()
        self.active_connections[user_id][chat_id] = websocket

    def disconnect(self, user_id: str, chat_id: str):
        if user_id in self.active_connections:
            self.active_connections[user_id].pop(chat_id, None)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]

    async def send_message(self, user_id: str, chat_id: str, message: dict):
        user_connections = self.active_connections.get(user_id, {})
        ws = user_connections.get(chat_id)
        if ws:
            try:
                await ws.send_json(message)
            except Exception:
                self.disconnect(user_id, chat_id)

    def is_connected(self, user_id: str, chat_id: str) -> bool:
        return chat_id in self.active_connections.get(user_id, {})

    async def send_notification(self, user_id: str, notification: dict):
        for ws in self.active_connections.get(user_id, {}).values():
            try:
                await ws.send_json({"type": "notification", "data": notification})
            except Exception:
                pass

    async def disconnect_user(self, user_id: str, code: int = 4003):
        connections = list(self.active_connections.get(user_id, {}).items())
        for chat_id, websocket in connections:
            try:
                await websocket.close(code=code)
            finally:
                self.disconnect(user_id, chat_id)


manager = ConnectionManager()


async def _allowed_presence_user_ids(db: AsyncSession, user_id: str, requested_user_ids: set[str]) -> set[str]:
    if not requested_user_ids:
        return set()

    user_uuid = UUID(user_id)
    matches_result = await db.execute(
        select(Match).where(
            ((Match.user_id == user_uuid) | (Match.owner_id == user_uuid)) &
            (Match.is_unmatched == False)
        )
    )
    allowed = set()
    for match in matches_result.scalars():
        other_user_id = str(match.user_id if match.owner_id == user_uuid else match.owner_id)
        if other_user_id in requested_user_ids:
            allowed.add(other_user_id)
    return allowed


async def handle_presence_websocket(websocket: WebSocket, token: str = Query(...)):
    payload = decode_token(token)
    if not payload or payload.get("type") != "access":
        await websocket.close(code=4001)
        return

    user_id = payload.get("sub")
    if not user_id:
        await websocket.close(code=4001)
        return

    async with async_session() as db:
        user = await db.get(User, UUID(user_id))
        if not user or not user.is_active:
            await websocket.close(code=4004)
            return

        connection_id = await presence_manager.connect_presence_socket(websocket, user_id)

        try:
            while True:
                data = await websocket.receive_json()
                message_type = data.get("type")
                if message_type == "subscribe":
                    raw_user_ids = data.get("user_ids") or []
                    requested_user_ids = {str(item) for item in raw_user_ids if item}
                    allowed_user_ids = await _allowed_presence_user_ids(db, user_id, requested_user_ids)
                    await presence_manager.subscribe(connection_id, allowed_user_ids)
                elif message_type == "ping":
                    await websocket.send_json({"type": "pong"})
        except WebSocketDisconnect:
            await presence_manager.disconnect(connection_id)
        except Exception as e:
            logger.error(f"Presence websocket error: {e}")
            await presence_manager.disconnect(connection_id)


async def handle_websocket(websocket: WebSocket, match_id: str, token: str = Query(...)):
    payload = decode_token(token)
    if not payload or payload.get("type") != "access":
        await websocket.close(code=4001)
        return

    user_id = payload.get("sub")
    if not user_id:
        await websocket.close(code=4001)
        return

    async with async_session() as db:
        try:
            match = await db.get(Match, UUID(match_id))
            if not match or match.is_unmatched:
                await websocket.close(code=4004)
                return

            user = await db.get(User, UUID(user_id))
            if not user or not user.is_active or not user.email_verified:
                await websocket.close(code=4004)
                return

            if user.id not in (match.user_id, match.owner_id):
                await websocket.close(code=4003)
                return

            other_uuid = match.user_id if user.id == match.owner_id else match.owner_id
            blocked = await db.scalar(
                select(UserBlock.id).where(
                    ((UserBlock.blocker_id == user.id) & (UserBlock.blocked_id == other_uuid))
                    | ((UserBlock.blocker_id == other_uuid) & (UserBlock.blocked_id == user.id))
                )
            )
            if blocked:
                await websocket.close(code=4003)
                return

            chat_result = await db.execute(select(Chat).where(Chat.match_id == UUID(match_id)))
            chat = chat_result.scalar_one_or_none()
            if not chat:
                chat = Chat(match_id=UUID(match_id))
                db.add(chat)
                await db.commit()

            other_user_id = str(match.user_id) if str(match.owner_id) == user_id else str(match.owner_id)

            await manager.connect(websocket, user_id, chat_id=match_id)

            mark_result = await db.execute(
                select(Message).where(
                    Message.chat_id == chat.id,
                    Message.sender_id != UUID(user_id),
                    Message.is_read == False,
                )
            )
            for msg in mark_result.scalars():
                msg.is_read = True
            await db.commit()

            if other_user_id in manager.active_connections and match_id in manager.active_connections[other_user_id]:
                await manager.send_message(other_user_id, match_id, {"type": "read_receipt", "by": user_id})

            while True:
                data = await websocket.receive_json()

                message_type = data.get("type")

                if message_type == "message":
                    content = (data.get("content") or "").strip()
                    if not content:
                        await websocket.send_json({"type": "error", "message": "Message cannot be empty"})
                        continue
                    if len(content) > 4000:
                        await websocket.send_json({"type": "error", "message": "Message is too long"})
                        continue

                    client_message_id = data.get("client_message_id")
                    if client_message_id is not None:
                        client_message_id = str(client_message_id)[:128]
                        existing_message = await db.scalar(
                            select(Message).where(
                                Message.sender_id == UUID(user_id),
                                Message.client_message_id == client_message_id,
                            )
                        )
                        if existing_message:
                            await websocket.send_json({
                                "type": "message_sent",
                                "data": {
                                    "id": str(existing_message.id),
                                    "chat_id": str(existing_message.chat_id),
                                    "sender_id": user_id,
                                    "content": existing_message.content,
                                    "is_read": existing_message.is_read,
                                    "created_at": existing_message.created_at.isoformat(),
                                    "sender_name": user.full_name,
                                    "client_message_id": client_message_id,
                                },
                            })
                            continue

                    recipient_is_active = manager.is_connected(other_user_id, match_id)
                    message = Message(
                        chat_id=chat.id,
                        sender_id=UUID(user_id),
                        content=content,
                        client_message_id=client_message_id,
                        is_read=recipient_is_active,
                    )
                    db.add(message)
                    await db.commit()
                    await db.refresh(message)

                    msg_data = {
                        "type": "new_message",
                        "data": {
                            "id": str(message.id),
                            "chat_id": str(chat.id),
                            "sender_id": user_id,
                            "content": message.content,
                            "is_read": recipient_is_active,
                            "created_at": message.created_at.isoformat(),
                            "sender_name": user.full_name,
                            "client_message_id": client_message_id,
                        },
                    }

                    await manager.send_message(other_user_id, match_id, msg_data)
                    await manager.send_message(user_id, match_id, {
                        "type": "message_sent",
                        "data": msg_data["data"],
                    })

                    if recipient_is_active:
                        await manager.send_message(user_id, match_id, {"type": "read_receipt", "by": other_user_id})

                    if other_user_id not in manager.active_connections or match_id not in manager.active_connections[other_user_id]:
                        db.add(OutboxEvent(
                            event_type="PUSH_NOTIFICATION",
                            payload={
                                "user_id": other_user_id,
                                "title": f"New message from {user.full_name}",
                                "body": message.content,
                            },
                        ))
                        await db.commit()

                elif message_type == "typing":
                    await manager.send_message(other_user_id, match_id, {
                        "type": "typing",
                        "user_id": user_id,
                    })

                elif message_type == "read":
                    unread_result = await db.execute(
                        select(Message).where(
                            Message.chat_id == chat.id,
                            Message.sender_id != UUID(user_id),
                            Message.is_read == False,
                        )
                    )
                    unread_messages = unread_result.scalars().all()
                    for msg in unread_messages:
                        msg.is_read = True
                    if unread_messages:
                        await db.commit()
                    await manager.send_message(other_user_id, match_id, {
                        "type": "read_receipt",
                        "by": user_id,
                    })

        except WebSocketDisconnect:
            manager.disconnect(user_id, match_id)
        except Exception as e:
            logger.error(f"WebSocket error: {e}")
            manager.disconnect(user_id, match_id)
