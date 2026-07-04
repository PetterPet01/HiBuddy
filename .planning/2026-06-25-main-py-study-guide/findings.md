# Findings: main.py Study Guide

## main.py structure
1. **Config Initialization**: Loads and validates environment variables upfront to fail fast.
2. **Lifespan Manager**: Uses `AsyncIOScheduler` to set up background jobs like outbox processing, connects to PostgreSQL, Milvus, Redis, and FCM. Closes them cleanly on shutdown.
3. **App Setup**: Configures CORS middleware.
4. **Router Inclusions**: Routes traffic to modular API files.
5. **WebSockets**: Defines stateful endpoints for chat and presence, extracting JWT tokens from query parameters for handshake validation.

## Deep Dive Learnings
- **Outbox Concurrency:** `process_outbox` uses PostgreSQL's `with_for_update(skip_locked=True)` to prevent multiple running instances of `main.py`'s background scheduler from sending duplicate FCM push notifications.
- **WebSocket Handshake Validation:** A connection is only accepted if the `Match` is active, the `User` is verified, and neither user has a record in `UserBlock` against the other.
- **Milvus Startup Check:** Uses a pure python socket with a 0.5s timeout. If Milvus fails to respond within 0.5s, the exception is swallowed by the `lifespan` manager, allowing the app to boot via Graceful Degradation.

## Interview Prep Findings
- **Security Handshake:** WebSocket authorization must query multiple tables (User block list, Verification status, and Match status) to guarantee that blacklisted or unverified connections are rejected at the ASGI handshake level before any WebSocket frames are accepted or processed.
- **Relational Integrity:** Using cascades on foreign keys (e.g., `ondelete="CASCADE"` on `user_id` and `project_id`) ensures that deleting a user or project automatically cleans up child tables (like `user_skills`, `project_members`, and `swipe_queue_items`) to avoid orphan records.

