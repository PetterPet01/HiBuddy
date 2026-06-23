# Feature List 01: Backend Architecture, Relational Schema, and Security

This document expands upon Study Guide 01 by providing an exhaustive list of features supported by the backend architecture, relational schema, and security mechanisms.

## 1. FastAPI Application Lifecycle and Initialization
*   **Asynchronous Request Handling:** High-throughput non-blocking request processing via ASGI/Uvicorn.
*   **Lifespan Management:** Coordinated startup/shutdown sequences:
    *   Database connection pooling initialization and teardown.
    *   Redis connection pool initialization and teardown.
    *   PyTorch Model warm-up for NLP tasks.
*   **Centralized Exception Handling:** Standardized error formatting (JSON responses) for database constraints, validation errors, and authentication failures.
*   **CORS Support:** Configurable cross-origin resource sharing middleware.

## 2. Relational Schema & Database Operations
*   **Alembic Migrations:** Version-controlled database schema changes.
*   **User Management System:**
    *   `User` entity storing authentication credentials, login attempts, and account locks.
    *   `UserProfile` entity linked 1-to-1 with `User`, storing bios, reputation, portfolio links, and Milvus embedding references.
    *   Soft deletion and cascade rules ensuring data integrity.
*   **Roles & Skills Catalog:**
    *   `RoleCatalog` and `SkillCatalog` enumerating available disciplines.
    *   `RoleSkillCatalog` mapping standard skills to specific roles.
    *   `UserRole` and `UserSkill` linking users to disciplines with defined proficiency levels.
*   **Project Ecosystem:**
    *   `Project` entity defining titles, goals, commitments, start/end dates, and owner references.
    *   `ProjectRoleSlot` specifying needed roles, required headcount, and filled counts.
    *   `ProjectRoleSkillRequirement` defining specific skill levels per role slot.
    *   `ProjectMember` tracking joined users and their statuses.
    *   Constraint checking (e.g., end dates strictly after start dates, capacity limits).
*   **Task Management & Kanban:**
    *   `Task` entity with deadlines, priorities, status flows (TODO, IN_PROGRESS, DONE).
    *   `TaskAssignment` linking multiple assignees per task.
    *   `TaskCheckoutHistory` tracking status changes for audit logs.
*   **Matchmaking & Swiping Interactions:**
    *   `SwipeAction` tracking user approvals (LIKE/PASS) on projects (and vice versa).
    *   `Match` entity created upon mutual LIKE actions.
*   **Evaluation & Feedback:**
    *   `ProjectEvaluation` for post-project scoring of collaborators.
    *   `AnonymousFeedback` facilitating safe critique sharing among team members.
*   **Chat Infrastructure (Database Side):**
    *   `Chat` entity linking to a Match.
    *   `Message` entity storing chat content with unique client IDs for deduplication.

## 3. Authentication & Security
*   **OAuth2 / JWT Token System:**
    *   Access tokens for stateless authentication (short expiration).
    *   Refresh tokens mapped to specific devices/sessions (long expiration, hashed).
*   **Sliding-Window Token Rotation:**
    *   Automatic refresh token invalidation upon use, issuing a new pair.
*   **Replay Attack Prevention:**
    *   Detection of reused refresh tokens triggering "Token Family" invalidation (force-logout across devices).
*   **Google OAuth Integration:**
    *   ID Token verification directly with Google.
    *   Automatic account provisioning/merging on initial sign-in.
*   **Brute-Force Protection:**
    *   Tracking `login_attempts` in the `User` table.
    *   Temporary account locks (`locked_until`) upon successive failures.
