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

    async def send_notification(self, user_id: str, notification: dict):
        for ws in self.active_connections.get(user_id, {}).values():
            try:
                await ws.send_json({"type": "notification", "data": notification})
            except Exception:
                pass


manager = ConnectionManager()


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
            if not match:
                await websocket.close(code=4004)
                return

            user = await db.get(User, UUID(user_id))
            if not user:
                await websocket.close(code=4004)
                return

            if user.id not in (match.user_id, match.owner_id):
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

                if data["type"] == "message":
                    message = Message(
                        chat_id=chat.id,
                        sender_id=UUID(user_id),
                        content=data["content"],
                    )
                    db.add(message)
                    await db.commit()

                    msg_data = {
                        "type": "new_message",
                        "data": {
                            "id": str(message.id),
                            "chat_id": str(chat.id),
                            "sender_id": user_id,
                            "content": message.content,
                            "is_read": False,
                            "created_at": message.created_at.isoformat(),
                            "sender_name": user.full_name,
                        },
                    }

                    await manager.send_message(other_user_id, match_id, msg_data)
                    await manager.send_message(user_id, match_id, {
                        "type": "message_sent",
                        "data": msg_data["data"],
                    })

                elif data["type"] == "typing":
                    await manager.send_message(other_user_id, match_id, {
                        "type": "typing",
                        "user_id": user_id,
                    })

        except WebSocketDisconnect:
            manager.disconnect(user_id, match_id)
        except Exception as e:
            logger.error(f"WebSocket error: {e}")
            manager.disconnect(user_id, match_id)
