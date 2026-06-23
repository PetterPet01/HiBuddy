# Study Guide 06: Infrastructure, Seeding, and Automated Testing

This final module outlines how to spin up the entire multi-service HiBuddy infrastructure locally, how the relational database is populated with realistic test scenarios, and how automated tests validate both client-side UI/UX components and backend business logic.

---

## 🐳 1. Multi-Container Infrastructure with Docker Compose

Running a modern full-stack application requires a stable development environment. HiBuddy packages its external infrastructure dependencies into a unified **Docker Compose** configuration located at `backend/docker-compose.yml`.

### System Services & Topology
When running `docker compose up -d`, 5 isolated service containers are created and bound to a local bridged network:

1.  **PostgreSQL (Port 5433)**: Persistent relational database. Migrations are managed by Alembic running from the host.
2.  **Redis (Port 6379)**: Fast, in-memory key-value cache used for tracking user online presence heartbeats.
3.  **etcd (Port 2379)**: Highly-available key-value metadata store. This acts as the consensus and configuration engine required specifically by the Milvus vector database cluster.
4.  **MinIO (Ports 9000/9001)**: S3-compatible local object storage server. It is utilized by Milvus to commit and persist its vector embedding segments and indices.
5.  **Milvus Standalone (Port 19530)**: High-performance vector search engine that stores the generated 384-dimensional user and project semantic vectors, running vector search algorithms (HNSW) over them.

---

## 🌱 2. Relational and Swiping Data Seeding

To verify that the platform functions correctly without manually typing profiles or swiping dozens of times, the system provides a comprehensive database seed script in `backend/seed_data.py`.

### What the Seed Script Simulates
Running `python seed_data.py` completes a sequential pipeline using the async SQLAlchemy connection:
1.  **Users (11 profiles)**: Inserts mock student accounts with pre-computed hashed passwords (`HiBuddyDemo!2026`). Mapped skills, user interests, chosen roles, and academic course tracks are automatically populated.
2.  **Projects (6 listings)**: Populates realistic group projects (e.g., *StudyMate - AI Study Planner* and *GreenTrack - Carbon Footprint App*) with descriptions, active fields, role slots (slots defined, capacity, filled), and commitments.
3.  **Project Members & Tasks**: Joins several users to projects, assigns active tasks, and logs historical task movements to generate timeline metrics.
4.  **Swipe Actions & Matches (18 actions)**: Populates mutual likes between contributors and projects, automatically triggering the generation of `Match` and `Chat` entities in the relational schema.
5.  **Chat Messages & Moderation**: Pre-populates sample WebSocket-style chat messages, student verification reviews, blocks, and user report cases to fully test Trust & Safety admin workflows.

---

## 🧪 3. Comprehensive Automated Testing Pipelines

Both client and server codebases implement automated testing suites to verify software correctness.

### Backend Testing (Pytest-Asyncio)
The python test suite resides in `backend/tests/` and is executed via `pytest`.
*   **Validation Tests (`test_validation.py`)**: Asserts that Pydantic validation schemas reject malformed data (e.g., weak passwords, invalid email formats, project roles with negative capacities).
*   **Email Verification Tests (`test_email_verification.py`)**: Tests async relational transactions, user registration locks, redirect blocks for unverified students, and OTP verification code lifecycles under async database sessions.
*   **Swipe Queue Tests (`test_swipe_queue.py`)**: Asserts the accuracy of swipe state-transitions and match-making triggers.
*   **Matching Tests (`test_matching.py`)**: Validates the deterministic multi-criteria scoring logic. It provides mock profiles and checks that computed Jaccard values and skill-matching percentages match expected scoring ranges.

### Client-Side Testing (Android & Compose)
The Android client implements both local JVM unit tests and device-connected instrumentation tests.
*   **JVM Unit Tests (`app/src/test/`)**:
    *   `SwipeDecisionResolverTest.kt`: Validates swipe mathematics. It simulates different touch gesture velocities and x/y directional drag offsets, asserting whether the system correctly interprets them as a `Like`, `Pass`, `SuperLike`, or a canceled action.
*   **On-Device UI Tests (`app/src/androidTest/`)**:
    *   `DiscoverScreenTest.kt`: Boots up a mockup Jetpack Compose environment to test view behaviors. It asserts that components like `StatChip`, `MetaTag`, and the swiping `EmptyStackView` render with correct labels, and verifies that clicking Header controls triggers the appropriate ViewModel callbacks.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "Why do we need etcd and MinIO in the docker-compose file? Aren't PostgreSQL and Redis enough?"
*   **Theoretical Answer**: While PostgreSQL manages structured relational tables and Redis manages real-time presence cache states, our application utilizes **Milvus** to handle vector searches for semantic matching. Milvus is a high-performance vector database that relies on a modular architecture. It is not self-contained; it utilizes **etcd** as its consensus engine to synchronize configuration metadata and node health across its internal services, and uses S3-compatible object storage (**MinIO** in our local environment) to persist raw segment files, index metadata, and vector log streams. Without these supporting services, Milvus cannot boot.
*   **Code Reference**: Open `backend/docker-compose.yml` and show the lecturer how the `milvus` service (Lines 67–88) is declared with explicit dependencies (`depends_on`) on both the `etcd` and `minio` containers, showing how the microservice cluster hangs together.

### Q2: "How do you test Jetpack Compose UI components when they don't have traditional XML Views with fixed ID tags?"
*   **Theoretical Answer**: Jetpack Compose is a declarative UI toolkit, so it does not support traditional `findViewById` selectors. Instead, Compose-based testing relies on the **Semantic Tree** and the `ComposeTestRule` API. Developers use semantic finders to locate nodes on the screen based on their text values, content descriptions, or custom **test tags** attached via modifiers. Once a node is located, the test library injects mock user actions (such as `performClick` or `performGesture`) and asserts that the layout state transitions correctly (e.g., verifying that a specific view is now displayed or hidden).
*   **Code Reference**: Refer to `/app/src/androidTest/java/com/example/hibuddy/ui/screens/DiscoverScreenTest.kt`. Point out how the test uses `composeTestRule` to query elements in the composable tree (e.g., finding controls by text or content description) and asserts interaction outcomes.

---

**Congratulations!** You have completed the HiBuddy Full-Stack Developer Study Guide. Use the custom prompt provided in the **[README Index](README.md)** with Claude in your self-exploration session to run mock interviews and review specific lines of code in real time!
