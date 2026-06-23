# Study Guide 01: Backend Architecture, Relational Schema, and Security

This module breaks down the foundation of the HiBuddy backend service, detailing how FastAPI acts as an asynchronous gateway, how the relational database maps out user profiles, projects, and activities, and how the OAuth2 authentication architecture guarantees secure, stateful operations.

---

## 🚀 1. FastAPI Application Lifecycle and Initialization

The entry point of the entire backend application is located in `backend/app/main.py`.

### Architectural Highlights
*   **Asynchronous ASGI Support**: Built entirely on **FastAPI** and served via **Uvicorn**, utilizing Python’s `asyncio` event loop. Every HTTP and WebSocket request runs non-blocking, maximizing request throughput.
*   **Lifespan Events**: FastAPI uses a `lifespan` context manager (`@asynccontextmanager`) to register startup and shutdown events cleanly:
    *   **Startup**: Connects to the PostgreSQL database, registers Redis cache pools, and warms up the PyTorch `SentenceTransformer` neural network models to prevent initialization latency during active user requests.
    *   **Shutdown**: Gracefully drains active client connections, flushes in-memory queues, and terminates SQLAlchemy connection pools and Redis socket buffers.
*   **Global Exception Filters**: Custom routers catch database constraints, validation schema formatting mismatches, and JWT token expirations to return standard, structured JSON error bodies to the mobile client (e.g., `401 Unauthorized` or `422 Unprocessable Entity`).

---

## 🗄️ 2. Core Relational Models & Schema Design

Database models are declared using modern async **SQLAlchemy ORM** configurations under `backend/app/models/`. Migrations are compiled and tracked via **Alembic** (`backend/alembic/versions/`).

### Entity Relationship Walkthrough
The following diagram showcases how core database models interlock:

```
+---------------+           +--------------------+           +----------------------+
|     User      |1 ------ 1 |    UserProfile     |1 ------ * |       UserSkill      |
|---------------|           |--------------------|           |----------------------|
| id (PK)       |           | user_id (FK)       |           | user_id (FK)         |
| email         |           | display_name       |           | skill_name (PK, FK)  |
| role          |           | bio                |           | skill_level          |
| verified_stud |           | embedding_id (Ref) |           +----------------------+
+---------------+           +--------------------+
        |1                             |1
        |                              |
        |* (Owner)                     |* (Swiper)
+---------------+           +--------------------+           +----------------------+
|    Project    |1 ------ * |    SwipeAction     | * ----- 1 |        Match         |
|---------------|           |--------------------|           |----------------------|
| id (PK)       |           | swiper_id (FK)     |           | id (PK)              |
| owner_id (FK) |           | target_project(FK) |           | contributor_id (FK)  |
| field         |           | type (LIKE/PASS)   |           | project_id (FK)      |
+---------------+           +--------------------+           +----------------------+
        |1                                                               |1
        |                                                                |
        |*                                                               |1
+-------------------+                                        +----------------------+
|  ProjectRoleSlot  |                                        |         Chat         |
|-------------------|                                        |----------------------|
| project_id (FK)   |                                        | match_id (PK, FK)    |
| role_name (PK, FK)|                                        +----------------------+
+-------------------+                                                    |1
                                                                         |
                                                                         |*
                                                             +----------------------+
                                                             |       Message        |
                                                             |----------------------|
                                                             | id (PK)              |
                                                             | chat_id (FK)         |
                                                             | client_msg_id (Unique)
                                                             +----------------------+
```

### Critical Database Tables & Files
*   **`User` & `UserProfile` (`backend/app/models/user.py` & `profile.py`)**: Split into two tables to keep the auth credentials lightweight. The `user_profiles` table stores the display name, professional bio, portfolio URL, matching mode (`CONTRIBUTOR`, `OWNER`, `BOTH`), reputation scores, and the `embedding_id` representing the corresponding dense vector index stored in Milvus.
*   **`Project` & `ProjectRoleSlot` (`backend/app/models/project.py`)**: A `Project` is created by a user of role `OWNER`. Each project lists several target roles inside the `project_role_slots` table (e.g., "Frontend Developer" with a quota of `count=1` and `filled=0`). When a contributor joins, the `filled` status is incremented, and an association record is written to `project_members`.
*   **`SwipeAction` & `Match` (`backend/app/models/swipe.py`)**: `SwipeAction` records individual swiping actions. When a contributor "likes" a project, a record of type `LIKE` is written. If the project owner subsequently swiping on the contributor yields a `LIKE` in the same context, a `Match` record is instantly created.
*   **`Chat` & `Message` (`backend/app/models/chat.py`)**: Every individual match spawns exactly one 1:1 `Chat` instance. The `Message` table houses individual messages, including a `client_message_id` UUID generated by the Android client to guarantee network deduplication.

---

## 🔑 3. Secure OAuth2 Authentication & Token Rotation

The authentication system is built from scratch under `backend/app/services/auth_service.py` to support standard credential-based security, sliding-window token rotations, and Google Single Sign-In integration.

### Core Architecture Flow
1.  **Google Identity Provisioning**: If a user signs in via Google OAuth, the Android client retrieves an ID Token from Google's credential system and hits `/api/auth/google`.
    *   The backend verifies the token directly with Google (`google.oauth2.id_token.verify_oauth2_token`).
    *   If the user does not exist in the local database, the backend automatically provisions a new `User` record, parses the Google profile photo, and marks the user's email verified since it has already been validated by Google.
2.  **Access & Refresh Token Issuance**: On successful verification or traditional login, the server issues a standard JSON payload:
    ```json
    {
      "access_token": "eyJhbGciOi...",
      "refresh_token": "g9z2x7k1...",
      "token_type": "bearer",
      "expires_in": 900
    }
    ```
    *   **Access Token**: A stateless, short-lived JWT token containing the `user_id` and role claims, with a strict 15-minute expiration window (`expires_in: 900`).
    *   **Refresh Token**: A long-lived, high-entropy random string (typically 30 days) stored as a secure SHA-256 hash in the `refresh_tokens` database table.
3.  **Sliding Window & Token Families (Token Rotation)**:
    *   When the access token expires, the client calls `/api/auth/refresh` passing the long-lived refresh token.
    *   **Rotation**: The backend verifies the refresh token hash, invalidates it, generates a brand new refresh token, saves the new hash, and returns a new Access/Refresh pair to the client. This implements **sliding-window security**.
    *   **Replay Protection**: If an attacker steals a refresh token and tries to reuse it, the server detects that the token has *already* been consumed. To protect the user, the server automatically invalidates the **entire refresh token family** (all active sessions belonging to that user), forcing an immediate re-authentication across all devices.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "Why did you separate the User table from the UserProfile table?"
*   **Theoretical Answer**: This is an application of **database normalization and memory optimization**. The `User` table contains high-security, frequently read auth credentials and access configurations (passwords, role claims, locking stats). Keeping it clean allows fast lookups during token validation without loading large user blobs (like biographies, profile photos, or vectors). The `UserProfile` contains dynamic, large-text metadata which is only needed when loading details or matching queues.
*   **Code Reference**: Refer to `backend/app/models/user.py` (`User` class, lines 12–50) and `backend/app/models/profile.py` (`UserProfile` class, lines 9–31). Notice how they are bound by a clear 1-to-1 foreign key relationship.

### Q2: "What is sliding-window token rotation, and how does your backend prevent Replay Attacks?"
*   **Theoretical Answer**: Token rotation means every time a client uses a `refresh_token` to get a new `access_token`, the server invalidates that specific `refresh_token` and issues a new one. This ensures refresh tokens are single-use. If a malicious entity intercepts a refresh token and tries to replay it, the server sees the token is already flagged as revoked. It flags this as a compromise event, lookup the token family chain, and instantly purges all refresh tokens for that specific user.
*   **Code Reference**: Open `backend/app/services/auth_service.py` and point out `refresh_access_token` (Lines 314–347). Show how the code queries the hash in the `refresh_tokens` database table, validates its active status, sets it as revoked, and raises an immediate `401 Unauthorized` while invalidating the family if a previously-used token is submitted.

---

*Move on to the next section: **[02. Hybrid Matching & Semantic Embeddings](02_matching_and_embeddings.md)** to learn how the algorithmic core works.*
