from __future__ import annotations

import uuid
from collections import defaultdict
from datetime import datetime, timezone

from fastapi import WebSocket


class PresenceManager:
    """Tracks live authenticated sockets for app/user presence.

    Presence is intentionally runtime state: a user is online only while at least
    one authenticated app presence websocket is currently connected.
    """

    def __init__(self):
        self._sessions_by_user: dict[str, set[str]] = defaultdict(set)
        self._presence_sockets: dict[str, WebSocket] = {}
        self._socket_users: dict[str, str] = {}
        self._watched_users: dict[str, set[str]] = defaultdict(set)
        self._last_seen_at: dict[str, datetime] = {}

    async def connect_presence_socket(self, websocket: WebSocket, user_id: str) -> str:
        await websocket.accept()
        connection_id = f"presence:{uuid.uuid4()}"
        self._presence_sockets[connection_id] = websocket
        self._socket_users[connection_id] = user_id
        await self.register_session(user_id, connection_id)
        return connection_id

    async def register_session(self, user_id: str, connection_id: str | None = None) -> str:
        connection_id = connection_id or f"session:{uuid.uuid4()}"
        was_offline = not self.is_online(user_id)
        self._sessions_by_user[user_id].add(connection_id)
        if was_offline:
            self._last_seen_at[user_id] = datetime.now(timezone.utc)
            await self._broadcast_presence(user_id)
        return connection_id

    async def disconnect(self, connection_id: str):
        user_id = self._socket_users.pop(connection_id, None)
        self._presence_sockets.pop(connection_id, None)
        self._watched_users.pop(connection_id, None)
        if user_id:
            await self.unregister_session(user_id, connection_id)

    async def unregister_session(self, user_id: str, connection_id: str):
        sessions = self._sessions_by_user.get(user_id)
        if not sessions:
            return

        sessions.discard(connection_id)
        if not sessions:
            self._sessions_by_user.pop(user_id, None)
            self._last_seen_at[user_id] = datetime.now(timezone.utc)
            await self._broadcast_presence(user_id)

    async def subscribe(self, connection_id: str, user_ids: set[str]):
        if connection_id not in self._presence_sockets:
            return

        self._watched_users[connection_id] = user_ids
        websocket = self._presence_sockets[connection_id]
        await self._send_json(
            websocket,
            {
                "type": "presence_snapshot",
                "users": [self.get_presence(user_id) for user_id in sorted(user_ids)],
            },
        )

    def is_online(self, user_id: str) -> bool:
        return bool(self._sessions_by_user.get(user_id))

    def get_presence(self, user_id: str) -> dict:
        last_seen = self._last_seen_at.get(user_id)
        return {
            "user_id": user_id,
            "is_online": self.is_online(user_id),
            "last_seen_at": last_seen.isoformat() if last_seen else None,
        }

    async def _broadcast_presence(self, user_id: str):
        payload = {
            "type": "presence",
            **self.get_presence(user_id),
        }
        stale_connections = []
        for connection_id, websocket in list(self._presence_sockets.items()):
            if user_id not in self._watched_users.get(connection_id, set()):
                continue
            sent = await self._send_json(websocket, payload)
            if not sent:
                stale_connections.append(connection_id)

        for connection_id in stale_connections:
            await self.disconnect(connection_id)

    async def _send_json(self, websocket: WebSocket, payload: dict) -> bool:
        try:
            await websocket.send_json(payload)
            return True
        except Exception:
            return False


presence_manager = PresenceManager()
