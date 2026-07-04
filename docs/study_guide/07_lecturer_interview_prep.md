# Study Guide 07: Lecturer Interview Prep (Q&A Checklist)

This guide acts as a mock interview simulator for **Module 01 (Backend Architecture & Models)** and **Module 03 (Real-time Sockets & Chat)**. It is structured with exact questions a lecturer is likely to ask during a technical review, followed by detailed, industry-standard answers referencing the HiBuddy codebase.

---

## 🗄️ Part 1: Relational Schema & Database Design (Module 01)

### Q1.1: "Your relational database has 35 tables. Why is the schema so fragmented? Why not store skills or interests as JSON or simple lists inside the `users` table?"
*   **Lecturer's Focus**: Testing your understanding of **Database Normalization (1NF, 2NF, 3NF)**, search optimization, and relational integrity.
*   **Your Answer**: 
    > "Having 35 tables is a result of keeping the database normalized (specifically in **3rd Normal Form**). 
    > If we stored skills or interests as lists or JSON arrays inside a single `users` table, we would encounter three major issues:
    > 1. **Query Performance**: Indexing JSON or string fields is highly inefficient. If we want to find all users with a specific skill (e.g., 'React'), the database would have to perform full-table scans. With normalized tables (`user_skills`, `skill_catalog`), we can create foreign key indexes to run this lookup in logarithmic ($O(\log N)$) time.
    > 2. **Data Consistency**: If a skill name is typed as a raw string, we will get typos (e.g., 'ReactJS', 'React', 'react'). By normalizing, we use a central `skill_catalog` where every skill has a unique, standardized ID. The table `user_skills` simply references this ID.
    > 3. **Update Anomalies**: If we need to rename a skill or delete a skill from the catalog, doing so in a single master catalog is instant. If it were embedded inside JSON lists across millions of user rows, we would have to rewrite every single user record."
*   **Code Reference**: Point to the 1-to-many relationship from `User` ([user.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/user.py#L48-L52)) to `UserProfile` ([profile.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/profile.py#L9-L35)) and `UserSkill` ([profile.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/profile.py#L57-L70)).

### Q1.2: "What cascade deletion strategy did you implement, and why is it important?"
*   **Lecturer's Focus**: Referential integrity and preventing orphaned data.
*   **Your Answer**: 
    > "We use `ondelete="CASCADE"` on our foreign keys linking child records to the parent entities (like `User` or `Project`).
    > For example, in `UserProfile`, `UserSkill`, `UserInterest`, and `FCMToken`, the foreign keys to `users.id` are configured with `ondelete="CASCADE"`. 
    > If a user decides to delete their account, PostgreSQL automatically deletes their profile, active skills, interests, and FCM tokens. This guarantees **referential integrity**, ensuring we don't have orphan rows pointing to non-existent user IDs, which would crash our query joins."
*   **Code Reference**: See `user_id` column in `UserSkill` ([profile.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/profile.py#L64)):
    ```python
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    ```

## 🧠 Part 1.5: Advanced Database & Concurrency

### Q1.3: "You're running an asynchronous background scheduler for pushing notifications (Outbox pattern). How do you prevent two background workers from grabbing and sending the exact same notification simultaneously?"
*   **Lecturer's Focus**: Database concurrency, transaction isolation, and locking mechanisms.
*   **Your Answer**: 
    > "To prevent race conditions in our distributed environment, we use PostgreSQL's **row-level locking** combined with the **Transactional Outbox Pattern**.
    > In our background job (`process_outbox`), when we query for pending notification events, we append `.with_for_update(skip_locked=True)` to the SQLAlchemy query.
    > 1. `FOR UPDATE` tells PostgreSQL to place a write lock on those specific rows so no other transaction can touch them.
    > 2. `SKIP LOCKED` is the secret sauce: if Worker A locks rows 1-50, and Worker B queries the outbox at the exact same millisecond, Worker B will *instantly skip* rows 1-50 and grab rows 51-100 instead of blocking and waiting.
    > This allows us to scale horizontally to dozens of backend instances without ever double-sending a push notification."
*   **Code Reference**: Point to `backend/app/services/outbox_service.py` where `with_for_update(skip_locked=True)` is called.

### Q1.4: "Why do you have two databases—PostgreSQL and Milvus? Why not just put everything in PostgreSQL?"
*   **Lecturer's Focus**: Polyglot persistence, vector embeddings, and appropriate tool selection.
*   **Your Answer**: 
    > "We practice **Polyglot Persistence**, using the right database for the right job:
    > 1. **PostgreSQL (Relational)** is our source of truth. It excels at ACID transactions, referential integrity (foreign keys), and structured queries (e.g., matching exact roles or checking verification status).
    > 2. **Milvus (Vector)** is a purpose-built database for Machine Learning. We use a neural network (PyTorch `SentenceTransformer`) to convert user bios and project descriptions into dense mathematical arrays (embeddings). 
    > We use Milvus because it uses specialized indexes (like `HNSW`) and metrics (like `COSINE` distance) to perform nearest-neighbor similarity searches across millions of vectors in milliseconds—something a traditional B-Tree index in PostgreSQL cannot efficiently do. We store the `embedding_id` in Postgres to map the two together."
*   **Code Reference**: Explain the separation by pointing to `app.database` (Postgres) and `app.milvus_client` (Milvus) initialization in `backend/app/main.py`.

### Q1.5: "What are Partial Indexes, and how do you use them in the `Match` table?"
*   **Lecturer's Focus**: Advanced index optimization and conditional constraints.
*   **Your Answer**: 
    > "A partial index is a database index built with a `WHERE` clause, meaning it only indexes a subset of the rows in the table.
    > In our `Match` table, a User and a Project should only have **one active match**. However, if they unmatch, we keep the record for history (soft deletion by setting `is_unmatched = True`). If we put a standard unique constraint on `(user_id, project_id)`, they could never match again in the future!
    > To solve this, we use a partial unique index: `postgresql_where=text("is_unmatched = false")`. 
    > This guarantees that there is strictly only one *active* match between a user and a project at any given time, while allowing infinite historical *inactive* matches to exist in the database without throwing a unique constraint violation."
*   **Code Reference**: Show the `__table_args__` on the `Match` class in [swipe.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/swipe.py#L36-L45).

### Q1.6: "Can you give me a full breakdown of the database technologies used? How many tables or collections exist in each, and why is the data model so heavily fragmented instead of using fewer, larger tables?"
*   **Lecturer's Focus**: Comprehensive architecture understanding, normalization vs. denormalization, and polyglot persistence.
*   **Your Answer**: 
    > "Our system utilizes a **Polyglot Persistence** architecture, dividing data between two distinct database engines based on their specific strengths. In total, we use **35 Relational Tables** in PostgreSQL and **2 Vector Collections** in Milvus.
    > 
    > **1. The Database Breakdown:**
    > *   **PostgreSQL (35 Tables)**: Handles our primary, structured business logic. It's strictly normalized into distinct domains: User Profiles (e.g., `users`, `user_profiles`, `user_skills`), Projects & Tasks (`projects`, `tasks`, `task_assignments`), Real-time Comms (`chats`, `messages`), and System Operations (`outbox_events`, `admin_audit_logs`). 
    > *   **Milvus (2 Collections)**: Handles high-dimensional data for our AI matching engine. We have `user_profile_vectors` and `project_vectors`. These store the mathematical embeddings generated from user bios and project descriptions, allowing us to perform semantic nearest-neighbor searches.
    > 
    > **2. Why so many tables? (The 'Fragmentation' Strategy):**
    > We deliberately 'fragmented' the PostgreSQL schema following **3rd Normal Form (3NF)** principles to achieve strict data governance:
    > *   **Many-to-Many Resolution**: Real-world entities rarely have 1-to-1 relationships. A project has many users, and a user joins many projects. We cannot store this as arrays in a single column without breaking SQL paradigms. Thus, we create junction tables like `project_members` and `task_assignments`.
    > *   **Master Data Catalogs**: Instead of letting users type free-text skills (causing typos like 'React' vs 'react.js'), we abstract them into a `skill_catalog`. Users link to this via `user_role_skills`. This guarantees data uniformity.
    > *   **Domain Isolation (Microservice-Ready)**: The codebase separates concerns. Authentication tokens (`refresh_tokens`) are isolated from profiles (`user_profiles`). This keeps our authentication queries extremely fast because they aren't loading large bio strings into memory.
    > 
    > **3. The Direct Benefits:**
    > *   **Preventing Update Anomalies**: If a skill name needs updating across the platform, we update exactly *one* row in `skill_catalog`, rather than millions of user rows.
    > *   **Query Optimization**: Highly targeted tables mean highly targeted indexes. We can do lightning-fast joins on `(user_id, project_id)` in `project_members` without dragging heavy textual descriptions into the query planner's memory.
    > *   **Cascading Lifecycles**: By linking fragmented tables with Foreign Keys and `ON DELETE CASCADE`, when a user is deleted, PostgreSQL automatically purges their skills, chat messages, and project memberships. We avoid orphaned data entirely."
*   **Code Reference**: 
    *   **Postgres Domains**: Point to the modularity in the `backend/app/models/` directory. For instance, [catalog.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/catalog.py) cleanly separates the `SkillCatalog` from `UserRoleSkill`.
    *   **Milvus Collections**: Point to [milvus_client.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/milvus_client.py#L56-L104) where `user_profile_vectors` and `project_vectors` are explicitly defined with schemas matching our PyTorch embedding dimensions.

### Q1.7: "I see you are using Alembic in your backend. Why did you choose to use a migration tool instead of just letting SQLAlchemy create all tables at startup using `Base.metadata.create_all()`?"
*   **Lecturer's Focus**: Schema version control, production readiness, and database evolution over time.
*   **Your Answer**: 
    > "We use **Alembic** to implement version control for our database schema, which is a critical requirement for production environments.
    > 
    > If we simply used `Base.metadata.create_all()`, SQLAlchemy could create tables initially, but it **cannot modify** existing tables. If we needed to add a new column to the `users` table or change an index later on, `create_all()` would just ignore it because the table already exists. We would be forced to manually write raw SQL `ALTER TABLE` scripts or drop and recreate the whole database—which would destroy all user data!
    > 
    > Alembic solves this by acting like Git for our database:
    > 1. **Autogeneration**: When we modify a Python model, we run `alembic revision --autogenerate`. Alembic compares our Python code against the live PostgreSQL schema and automatically generates the exact `ALTER TABLE` SQL needed.
    > 2. **Upgrades & Downgrades**: It generates Python migration scripts with `upgrade()` and `downgrade()` functions. If a deployment fails, we can instantly rollback the schema to the previous version.
    > 3. **History Tracking**: Alembic creates an `alembic_version` table in Postgres to track exactly which migration was last applied, ensuring developers stay synchronized without overriding each other's schema changes."
*   **Code Reference**: 
    *   Point to `backend/alembic/env.py` where we pass `target_metadata = Base.metadata` so Alembic can read our models.
    *   Point to the `backend/alembic/versions/` directory, which acts as the chronological history of all database schema changes over the project's lifetime.

---

## 🔑 Part 2: Authentication, Security & Token Rotation (Module 01)

### Q2.1: "Explain how your authentication system prevents JWT theft. What is sliding-window token rotation and how do you handle replay attacks?"
*   **Lecturer's Focus**: Stateless vs. stateful authentication, session security, and compromise detection.
*   **Your Answer**: 
    > "We implement **OAuth2 with Sliding-Window Token Rotation and Replay Protection**:
    > 1. **Short-Lived Access Tokens**: Access tokens are stateless JWTs containing user identity claims with a short expiration of 15 minutes. Even if stolen, the attacker's window of opportunity is extremely limited.
    > 2. **Sliding-Window Refresh Tokens**: To keep the user logged in without prompting them every 15 minutes, we issue a long-lived `refresh_token` (random secure string). When the access token expires, the client submits the refresh token to get a new pair.
    > 3. **Single-Use & Rotation**: Every time a refresh token is used, the server immediately revokes it and issues a brand new one.
    > 4. **Token Reuse Detection (Replay Attacks)**: If an attacker steals a used refresh token and attempts to submit it, the server checks the database and sees that the token's status is already set to `revoked`. 
    > To protect the user, we assume a breach has occurred. The server automatically lookup the **token family** and invalidates *every* refresh token associated with that user, immediately logging them out of all devices."
*   **Code Reference**: 
    *   `RefreshToken` model: [chat.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/chat.py#L61-L74)
    *   Check the columns `token_family`, `is_revoked`, and `replaced_by_jti_hash` which track the lineage of token replacements.

### Q2.2: "How does the Google Sign-In flow work? How do you ensure it is secure?"
*   **Lecturer's Focus**: Safe integration with third-party identity providers.
*   **Your Answer**: 
    > "We use **OAuth2 ID Token Verification**:
    > 1. The Android mobile client authenticates with the Google SDK and receives a signed **ID Token** directly from Google.
    > 2. The client sends this token to our backend endpoint `/api/auth/google`.
    > 3. The backend does *not* trust the client's word. It uses the `google-auth` library to verify Google's cryptographic signature on the ID Token.
    > 4. Once validated, we extract the secure email, full name, and avatar URL. If they don't have an account, we automatically provision a new user row and set `email_verified=True` because Google has already validated their email ownership."

---

## 📡 Part 3: WebSockets & Real-Time Presence (Module 03)

### Q3.1: "How do you authorize WebSocket connections? WebSockets don't easily support HTTP Headers during the handshake—how do you get around this?"
*   **Lecturer's Focus**: Hands-on network protocol limits and security checks at connection time.
*   **Your Answer**: 
    > "During the initial WebSocket HTTP Handshake, mobile clients cannot easily attach standard `Authorization: Bearer <token>` headers. 
    > To circumvent this, the client passes the JWT access token as a **Query Parameter** (e.g., `/ws/chat/{match_id}?token=eyJ...`).
    > In our socket handler (in `websocket.py`), we extract this token from the query params before accepting the connection. We verify the signature and ensure:
    > 1. The user is a member of the requested `match_id`.
    > 2. Both users are verified.
    > 3. Neither user has blocked the other (checking the `user_blocks` table).
    > If any check fails, we reject the connection immediately during the handshake with a `4003 Forbidden` WS close code, preventing unauthorized frame parsing."
*   **Code Reference**: Check the WebSocket endpoint setup in `backend/app/api/websocket.py`.

### Q3.2: "Is your `ConnectionManager` class thread-safe? How would it behave if we scaled the backend to run on 5 server instances?"
*   **Lecturer's Focus**: Concurrency, event loops, and horizontal scaling.
*   **Your Answer**: 
    > "Yes, in the context of FastAPI and ASGI. FastAPI runs on **Uvicorn**, which utilizes Python's single-threaded, event-driven `asyncio` event loop. Since all WebSocket operations in Python run concurrently on a single thread rather than across multiple OS threads, our in-memory `ConnectionManager` (storing active sockets in a dictionary) does not suffer from typical thread race conditions.
    > However, if we scaled the app horizontally across **5 server instances** (behind a load balancer), an in-memory dictionary would fail because User A might be connected to Server 1, and User B to Server 2. 
    > To resolve this, we would implement a **Redis Pub/Sub** message broker. When a message is sent, the server publishes it to Redis under a chat channel. All 5 server instances subscribe to Redis, and whichever server holds the active WebSocket connection for the recipient delivers it. This decouples presence and connection state from individual server memory."
*   **Code Reference**: Show the `ConnectionManager` in `backend/app/api/websocket.py` using in-memory dicts.

---

## 💬 Part 4: Chat States & Offline Delivery Synchronization (Module 03)

### Q4.1: "If the network drops while a user sends a message, how does your system guarantee the message isn't lost or duplicated when they reconnect?"
*   **Lecturer's Focus**: Idempotency, local cache sync, and transaction handling.
*   **Your Answer**: 
    > "This is handled by **Client-Generated UUIDs and Database Idempotency**:
    > 1. **Client-Side Generation**: The mobile app generates a unique `client_message_id` (UUIDv4) and saves the message in its local SQLite database with a status of `Sending`.
    > 2. **Network Recovery**: If the connection drops, the client queues the message. Once the network is restored, it retries sending the payload containing this ID over the WebSocket.
    > 3. **Server-Side Idempotency**: When the server receives the message, it checks the database for `client_message_id`. If it already exists (meaning the server received it before the drop, but the client didn't get the ACK), the database's unique constraint prevents a duplicate insert. 
    > 4. The server simply ignores the duplicate insert, flags the message as successfully sent, and returns the ACK to the client to update its local state to `Sent`."
*   **Code Reference**: Point to the unique constraint on `client_message_id` inside `Message` ([chat.py](file:///home/pet/AndroidStudioProjects/HiBuddy/backend/app/models/chat.py#L20)).

### Q4.2: "What is your fallback strategy if you send a message to a user who is offline? How do they actually get the message?"
*   **Lecturer's Focus**: REST synchronization, Push notification routing, background tasks, and SMTP failover.
*   **Your Answer**: 
    > "We use a multi-tiered offline synchronization fallback. The most important thing to remember is that **the database is the ultimate source of truth**. 
    > 1. **WebSocket Active Connection Check**: When a message is processed, the server checks if the recipient's user ID is registered in the in-memory `ConnectionManager`.
    > 2. **REST API Sync (The Core Mechanism)**: If the user is offline, the message sits safely in PostgreSQL. When the user later opens the app (or reconnects), the client calls the REST endpoints `/api/v1/chat/inbox` and `/api/v1/chat/{match_id}/messages` to pull down all missed messages and update its local SQLite database.
    > 3. **Tier 1 - Push Notification (FCM)**: To actually *tell* the user they have a message waiting (so they open the app and trigger that REST sync), the server routes a wakeup payload through the **Firebase Cloud Messaging (FCM)** service using the `firebase-admin` SDK and the user's registered device token.
    > 4. **Tier 2 - Email Fallback (SMTP)**: If the user has no registered FCM tokens or FCM fails, the backend falls back to sending an email notification via our asynchronous SMTP service to alert them."

---

## 🏆 Part 5: The Capstone Question

### Q5.1: "What did you find to be the most technically challenging aspect of your work? Explain in detail why it was complex."
*   **Lecturer's Focus**: Architectural thinking, problem-solving, understanding of distributed systems and edge cases.
*   **Your Answer**:
    > "The most technically challenging aspect of my work was engineering the **Guaranteed Message Delivery System and Offline Synchronization Strategy** for the chat module.
    > 
    > It was complex because it required bridging stateless REST APIs, stateful WebSockets, and asynchronous background workers, all while handling unpredictable mobile network drops.
    > 
    > Here is exactly why it was difficult:
    > 
    > 1. **The 'Two Generals' Problem (Network Drops)**: If a user sends a message, and their phone loses signal *after* the server receives it but *before* the server sends the acknowledgment (ACK) back, the phone thinks the message failed. When the signal returns, the phone automatically retries. Without protection, the server would insert a duplicate message. I solved this by implementing **Idempotency Keys**: the client generates a unique `client_message_id` (UUIDv4). The database enforces a unique constraint on this. If the server gets a retry, it catches the constraint violation, ignores the insert, and safely returns the ACK without duplicating the message in the UI.
    > 2. **Statefulness in a Stateless World**: WebSockets hold a persistent TCP connection in server memory. If the server restarts or scales horizontally, that memory is lost. I had to design the system so that the **PostgreSQL database remains the absolute source of truth**, not the socket memory. The socket is just a 'delivery pipe'.
    > 3. **The Multi-Tiered Asynchronous Fallback**: Ensuring an offline user gets a message wasn't as simple as just 'sending a push notification'. I had to orchestrate a distributed flow without blocking the main event loop:
    >    * First, transactionally insert the message into PostgreSQL.
    >    * Second, check the in-memory socket manager to see if they are active.
    >    * Third, if they are offline, queue an event in the `outbox_events` table (The Transactional Outbox Pattern) rather than calling Firebase directly. Calling external APIs in the chat route would cause massive lag.
    >    * Fourth, a background worker uses `.with_for_update(skip_locked=True)` to safely grab the event without conflicting with other workers, and triggers the Firebase (FCM) wakeup payload.
    >    * Finally, design the mobile client to wake up from that FCM payload and hit the REST API to perform a full database sync.
    > 
    > It was incredibly challenging because a failure at *any* of these steps—from the client UUID generation to the background worker lock—results in either lost messages, duplicate messages, or blocked server threads. Building it required a deep understanding of transactional boundaries and distributed idempotency."

---

## 💡 Quick Tips to Impress Your Lecturer
1.  **Use terms like 'Fail-Fast' and 'Stateless vs Stateful'**: Explain that we initialize settings upfront to fail-fast on startup, and that we balance stateless JWTs with stateful refresh token lists in PostgreSQL for secure rotations.
2.  **Point to specific files**: When explaining a concept, tell them exactly where the file is (e.g., *"This is implemented in `backend/app/api/websocket.py` using our `ConnectionManager` singleton"*).
3.  **Explain the 'Why' over 'What'**: Don't just say *"We use WebSockets."* Say *"We use WebSockets to provide bidirectional, low-latency communication, avoiding the server-overhead of continuous HTTP polling."*
