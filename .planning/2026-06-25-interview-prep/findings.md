# Findings: HiBuddy Architecture and Interview Prep

## Project Architecture
- **Frontend**: Android app written in Kotlin, uses Jetpack Compose for UI. Incorporates Firebase Cloud Messaging for Push Notifications.
- **Backend**: Python FastAPI with SQLAlchemy (PostgreSQL).
- **Core Features**: Project posting, matching students to role slots, swipe mechanism (mutual matching), chat, outbox pattern for reliable async notifications.
- **AI/ML Integration**:
  - **Mistral (LLM)**: Used for content moderation (parsing project descriptions to flag spam/fraud) and skill gap analysis from peer feedback.
  - **Milvus (Vector DB)**: Used for similarity search and discovering projects/candidates via embeddings.
- **Async Pattern**: Instead of direct API calls, uses an Outbox pattern where Notification objects are saved in DB transactionally with the Match, and a background `task_scheduler.py` polls and sends via `fcm_service.py`.

## Key Technical Difficulties
1. Race conditions during simultaneous mutual swipes. Handled via DB transactions and unique constraints.
2. Unreliable 3rd-party push notifications. Handled via Outbox Pattern.
3. LLM latency and hallucination. Handled via strict prompt schemas (JSON extraction), low temperature (0.1), timeouts (30.0s), and graceful fallbacks.
4. Scale of fuzzy matching. Handled via Milvus for fast vector similarity search instead of O(N) SQL LIKE.

## Dissemination Strategy
Translate the 27 tips from the provided thesis defense transcript into HiBuddy's context. Emphasize the microservice-lite architecture, the transaction safety of the matching engine, and the defensive design around ML components.
