# Study Guide 03: Real-Time Sockets, Presence, and Chat Synchronization

Real-time capabilities (messaging, dynamic user presence, typing indicators, and push notifications) form the backbone of student collaboration in HiBuddy. This module describes how the backend handles persistent state across WebSockets and coordinates offline delivery fallback mechanisms.

---

## 📡 1. The Dynamic WebSocket Connection Manager

The real-time chat architecture is designed around asynchronous **WebSockets (ASGI)** in FastAPI, implemented in `backend/app/api/websocket.py`.

To handle connections securely and concurrency-safe, the system registers a singleton **`ConnectionManager`**:

### In-Memory State Registry
*   **Data Structure**: The manager holds a nested dictionary:
    ```python
    self.active_connections: dict[int, dict[int, WebSocket]] = {}
    # Maps: User_ID -> { Chat_ID (Match_ID) -> WebSocket Connection Object }
    ```
*   **Security Validation**: When an Android client requests a WebSocket connection (`ws/chat/{match_id}`), the server parses the standard JWT token from the query parameters. It extracts the `user_id` and checks whether the user is a verified member of that specific `match_id` in PostgreSQL. If unauthorized, the socket is instantly closed with a `4003 Forbidden` WS closure code.
*   **Reconnection Resilience**: If a socket drops and reconnects, the manager overwrites the mapping safely, purging old disconnected references to prevent memory leaks.

---

## 🟢 2. User Presence and Broadcast Engine

Tracking who is currently online or offline is handled by the **`PresenceManager`** in `backend/app/services/presence_service.py` and routed via `/ws/presence`.

### The Lifecycle of User Presence
1.  **Online State Entry**: When a user launches the app, they establish a connection to the dedicated presence socket. The server writes a key to **Redis**: `presence:user_id` with a values containing current epoch time, setting a Time-to-Live (TTL) of 60 seconds.
2.  **Heartbeat Loop**: The Android client schedules a background job that emits a `ping` packet every 20 seconds. The server intercepting the ping resets the TTL of the key in Redis back to 60 seconds.
3.  **Active Broadcast**: When a state changes (User goes online or offline), the server looks up the user's active matches. It queries the `active_connections` registry of those friends and sends a JSON payload:
    ```json
    { "type": "presence_change", "user_id": 104, "status": "online" }
    ```
4.  **Graceful and Ungraceful Exit**:
    *   *Graceful*: If the user closes the app cleanly, the client disconnects, and the server removes the Redis key and broadcasts `"offline"`.
    *   *Ungraceful* (e.g., loss of cellular signal): The background heartbeat stops. Within 60 seconds, Redis expires the key. A background worker detects this expiration, cleans up connection maps, and broadcasts `"offline"`.

---

## 💬 3. Chat State Machine and Offline Notification Fallbacks

A robust instant messenger requires reliable states for messages. In HiBuddy, messages transition through several delivery phases:

$$\text{Draft} \implies \text{Sending} \implies \text{Sent (Stored in DB)} \implies \text{Delivered (Received by other client)} \implies \text{Read (Viewed by recipient)}$$

### Message Delivery Flow and Deduplication
1.  **Client Generation**: The Android client generates a local UUID `client_message_id`. This ID is stored in the local SQLite database as `Sending`.
2.  **Transmission**: The client emits the message JSON via the active WebSocket.
3.  **Deduplication & Storage**: The FastAPI server receives the payload. It runs a transaction checking if a message with that `client_message_id` already exists in PostgreSQL (to prevent duplicate entries under bad connection retries). It inserts the message with `read=False`.
4.  **Active Route (User Online)**: If the recipient's connection is active in the `ConnectionManager`, the server forwards the message instantly. The recipient's client acknowledges receipt by emitting a `read_receipt` or `delivery_receipt` packet back over the socket.
5.  **Passive Route (User Offline - Push Notifications & REST Sync)**:
    *   If the recipient has no active WebSocket connection, the socket broadcast fails.
    *   **The REST API Sync**: The ultimate source of truth is the PostgreSQL database. When the offline user later opens the app, their client calls the `/api/v1/chat/{match_id}/messages` or `/api/v1/chat/inbox` REST endpoints to fetch all messages they missed and store them in their local SQLite cache.
    *   **Tier 1 Fallback - Firebase Cloud Messaging (FCM)**: To prompt the user to open the app (or to wake the app up in the background to pre-fetch via the REST API), the server invokes `fcm_service.py` to send a push notification.
    *   **Tier 2 Fallback - Email Notification**: If FCM is unavailable or the user hasn't registered a device token, the system sends an email notification via SMTP (`email_service.py`) alerting them of the new message.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "How does your chat system handle thread safety and multi-user concurrency during WebSocket broadcasting?"
*   **Theoretical Answer**: Python is single-threaded but utilizes asynchronous concurrency via **ASGI (Uvicorn)** and `asyncio`. The `ConnectionManager` is designed as an async-safe class. Every connection write operation is wrapped in a non-blocking `await websocket.send_text()`. In-memory access to the `active_connections` dictionary is synchronous and single-threaded within the event loop, meaning there are no native OS-thread race conditions. For distributed multi-instance architectures where different users are connected to different physical backend servers, a **Redis Pub/Sub** mechanism can be layered over the registry to synchronize message broadcasts across servers.
*   **Code Reference**: Refer to `backend/app/api/websocket.py` (lines 23–65). Explain the `ConnectionManager` structure and show how its `broadcast` and `send_personal_message` methods are declared using Python's `async/await` syntax.

### Q2: "What happens if a mobile client loses connection mid-flight? How do you prevent message loss and duplicate messages?"
*   **Theoretical Answer**: This is handled by **Client-Generated UUIDs and Database Idempotency**. The mobile client does not wait for the server to generate a message ID. Instead, it generates a unique `client_message_id` (UUIDv4) locally. If the connection drops mid-flight, the client queues the message locally. Upon reconnection, it retries sending. If the server had already processed the message before the connection dropped, the unique constraint on `client_message_id` in PostgreSQL triggers a conflict block. The server safely ignores the duplicate insert, acknowledges the message as successfully delivered, and returns the existing record back to the client. This implements **Idempotency**.
*   **Code Reference**: Open `backend/app/models/chat.py` (Line 24). Point out the `client_message_id` column with its `unique=True` and `nullable=False` modifiers. Then open `backend/app/api/websocket.py` (Lines 227–235) to show how incoming messages are written to the database under this safety guard.

---

*Move on to the next section: **[04. Android Client Architecture & Offline Cache](04_android_app_architecture.md)** to see how the client application implements these features.*
