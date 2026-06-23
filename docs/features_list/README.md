# HiBuddy Exhaustive Features List

Welcome to the **HiBuddy Exhaustive Features List**. This directory was created as an expanded companion to the Study Guide. While the study guide focuses on architectural decisions, the "why/how", and interview preparation, this set of documents provides an exhaustive, granular list of every distinct feature implemented across the application.

## 🗺️ Master Features Index

1. **[01. Backend Architecture, Relational Schema, and Security](01_backend_architecture_and_models.md)**
   * Outlines the FastAPI application structure, database model relationships, and OAuth2 security flow.
2. **[02. Hybrid Matching and Semantic Vector Embeddings](02_matching_and_embeddings.md)**
   * Details the components of the deterministic matching score and the Milvus/SentenceTransformer AI vectorization pipeline.
3. **[03. Real-Time Communication and Chat State Sync](03_realtime_websocket_and_chat.md)**
   * Lists features concerning WebSockets, message delivery/read states, and presence management.
4. **[04. Android Client Architecture and Offline Cache](04_android_app_architecture.md)**
   * Covers Jetpack Compose UI architecture, dependency injection, token rotation, and Room local databases.
5. **[05. Admin, Moderation, and Trust Systems](05_admin_moderation_and_safety.md)**
   * Describes the reporting mechanisms, flagging states, admin controls, and academic email verification safeguards.
6. **[06. Infrastructure, Seeding, and Automated Testing](06_deployment_seeding_and_testing.md)**
   * Summarizes the Docker setup, the comprehensive seeding script, and testing strategies across both backend and client layers.
