# Study Guide 05: Admin, Moderation, and Trust Systems

For a student matchmaking application, maintaining trust and safety across teams is paramount. HiBuddy integrates a comprehensive Trust & Safety ecosystem that governs moderation pipelines, user reporting, and academic student email validation.

---

## 🛡️ 1. Flagging, Reporting, and Project Moderation

The backend includes structural workflows designed to process offensive profiles, spam projects, or toxic team behavior.

### Flagging and Moderation Workflow
*   **Reporting Pipeline**: Users of any role can file reports against offensive projects or user profiles. This writes a record to the `reports` table, containing the reporter's ID, the reported entity, a category (e.g., `SPAM`, `HARASSMENT`, `INAPPROPRIATE_CONTENT`), and detailed notes.
*   **Automatic Flagging**: When a project reaches a threshold of 3 reports, its database status automatically transitions to `FLAGGED_PENDING_REVIEW`.
*   **State Exclusion**: Once flagged, the project is instantly excluded from the general discovery algorithm. It cannot appear in any user's swiping deck, and existing matches related to it are temporarily suspended until a moderator resolves the case.
*   **Moderator Controls**: Admin users access a dedicated Admin Dashboard (routed via `/admin` in both backend and Android client layers):
    *   **Approve**: Clears all reports, resets flags, and restores the project to the matching queue.
    *   **Ban / Delete**: Permanently flags the project as `BANNED`, deletes its semantic vector index from Milvus, and triggers cascading cleanups to remove active chat records and pending matches.

---

## 🎓 2. Academic Student Email Verification

To prevent external malicious entities or bots from entering the student ecosystem, HiBuddy enforces a strict student validation flow using institutional email domains.

### The Verification Lifecycle
1.  **Registration Validation**: During user registration (`/api/auth/register`), the system requires a valid email.
2.  **Domain Guarding**: The backend checks the domain extension of the email against an institutional database. Standard addresses like `@gmail.com` or `@yahoo.com` are blocked or restricted from student features; only institutional addresses (such as `@university.edu` or local academic domains) are authorized to complete student registrations.
3.  **One-Time Code (OTP) Delivery**: The backend auth service generates a high-entropy, 6-digit numeric verification code with an expiration window of 10 minutes. This code is stored in the PostgreSQL database and sent to the user's institutional inbox via secure SMTP (`backend/app/services/email_service.py`).
4.  **Database Status Update**: When the user submits the code to `/api/auth/verify-email`, the backend validates the OTP and updates the `User` table:
    *   Sets `email_verified = True`
    *   Sets `verified_student = True`
5.  **State-Level Access Restrictions**: Within the Android navigation framework (`MainActivity.kt` and `ProfileCatalog`), users without `verified_student = True` are restricted from swiping on projects, creating groups, or starting chats, keeping the network secure.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "How do you ensure that a flagged project is instantly hidden from all user feeds without running heavy, database-taxing queries?"
*   **Theoretical Answer**: This is achieved using **Relational Join Filters and Indexing**. Inside the discovery query logic (`_discover_projects`), the SQL engine does not perform a full-table scan. Instead, the query joins the `projects` table with the `reports` table or filters directly on the `Project.status` column (which has a database index). The active SQL filter requires `Project.status == 'RECRUITING'`. When a project is flagged, its status changes to `FLAGGED_PENDING_REVIEW`, automatically excluding it from all active query results. Additionally, a similar filter is passed to the Milvus vector search engine as an index-matching expression, preventing Milvus from wasting CPU cycles evaluating similarity for banned or flagged cards.
*   **Code Reference**: Refer to `/backend/app/services/swipe_service.py` (`_discover_projects` method). Point out where the filters are compiled to exclude non-active or flagged projects.

### Q2: "What mechanisms prevent OTP (One-Time Password) brute-forcing on your email verification endpoint?"
*   **Theoretical Answer**: The backend implements **Rate Limiting, Absolute TTLs, and One-Time Consumption** policies:
    1.  **Absolute TTL**: Every generated verification code is saved with an absolute timestamp of 10 minutes. If the user submits the correct code after 10 minutes, the database rejects the transaction as expired.
    2.  **One-Time Consumption**: Once a verification code is successfully validated, it is instantly deleted or flagged as used in the database, preventing replay attempts.
    3.  **Rate Limiting**: The verification endpoints are protected by rate limiters that track attempts by IP and email. If a client submits incorrect verification attempts multiple times in succession, the backend blocks the IP for an exponential backoff period, mitigating brute-force attacks.
*   **Code Reference**: Refer to `/backend/app/services/auth_service.py` under the OTP generation and verification methods, detailing how timestamps are evaluated.

---

*Move on to the next section: **[06. Infrastructure, Seeding, & Testing](06_deployment_seeding_and_testing.md)** to understand how to spin up, populate, and test the full-stack system.*
