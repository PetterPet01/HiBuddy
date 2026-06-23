# Feature List 06: Infrastructure, Seeding, and Automated Testing

This document expands upon Study Guide 06 by providing an exhaustive list of features supported by local infrastructure, testing, and database initialization pipelines.

## 1. Multi-Container Infrastructure
*   **Docker Compose Configuration:** Unified setup for all dependencies.
*   **PostgreSQL Container:** Relational data persistence with Alembic volume mapping.
*   **Redis Container:** Fast key-value cache for session and real-time state.
*   **Milvus Cluster Support:**
    *   **etcd:** Consensus and metadata coordination for Milvus nodes.
    *   **MinIO:** S3-compatible object storage for vector segments and index files.
    *   **Milvus Standalone:** The core vector database engine.
*   **Network Bridging:** Isolated internal network facilitating inter-container communication.

## 2. Relational and Swiping Data Seeding
*   **Comprehensive `seed_data.py` Script:** Automated population of realistic test scenarios.
*   **Mock User Profiles:** Pre-computed hashed passwords, skills, interests, and academic tracks.
*   **Mock Project Listings:** Populating titles, role slots, commitments, and detailed descriptions.
*   **Team Simulation:** Assigning members to projects, generating tasks, and logging timeline history.
*   **Interaction Emulation:** Simulating swipe actions (Likes/Passes) to automatically trigger Match and Chat creation.
*   **Safety Pipeline Emulation:** Pre-populating chat messages, reports, blocks, and verification states for moderation testing.

## 3. Automated Testing Pipelines
*   **Backend Testing (Pytest-Asyncio):**
    *   **Schema Validation Tests:** Asserting strict Pydantic requirements (passwords, formats, negative capacities).
    *   **Async Transaction Tests:** Verifying email verification locks, OTP lifecycles, and rollback integrity.
    *   **Matching Algorithm Tests:** Validating deterministic score outputs (Jaccard values, skill percentages) against known constants.
    *   **API Endpoint Tests:** Utilizing HTTPX for integration testing of routing logic.
*   **Client-Side Testing (Android & Compose):**
    *   **JVM Unit Tests:** Verifying math and logic (e.g., gesture velocity calculations in `SwipeDecisionResolverTest.kt`).
    *   **Instrumented UI Tests:** Utilizing `ComposeTestRule` and semantic node finders to assert layout states, view visibility, and interaction callbacks.
