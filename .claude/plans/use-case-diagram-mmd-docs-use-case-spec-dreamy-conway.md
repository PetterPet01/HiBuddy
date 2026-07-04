# HiBuddy Use Case Demo Flow - Theatrical Plan

## Context
The goal of this plan is to define a highly practical, comprehensive "theatrical" run-through of the HiBuddy application's primary use cases, as defined in `use_case_diagram.mmd` and `docs/USE_CASE_SPECIFICATION.md`. We will utilize the existing FastAPI backend, the robust `seed_data.py` script, and Android client functionality to orchestrate a seamless narrative flow from onboarding through project matching, task management, AI-driven suggestions, and administrative safety moderation.

## Environment Prerequisite Orchestration

Before the demo begins, the database and AI embedding collections must be cleanly reset and seeded with known entities.

**Execution Script:**
```bash
# This script handles dropping/creating SQL tables and resetting Milvus collections
python backend/reset_database.py --reset all --yes
```

This guarantees the existence of specific seed personas we will use:
- **U1 (Minh Nguyen):** Project Owner of P4 (DevPort).
- **U2 (Thu Le):** UI/UX Designer, potential match for U1.
- **U4 (Linh Pham):** Guest/New User signing up.
- **U8 (Lan Tran):** User with a history of `LATE` tasks (T8, T10), perfect for demonstrating AI course suggestions.
- **U9 (Admin):** System administrator.

---

## The Demo Script (Acts / Scenes)

### 🎬 Scene 1: The First Impression - Guest Onboarding
*   **Focus Use Cases:** `Đăng ký` (DK), `Xác thực email` (XTE).
*   **Actor:** Guest (acting as Linh Pham - U4).
*   **Action:**
    1. Demonstrate the signup flow via the Android app.
    2. Show the OTP email verification step (`XTE`).
    3. The Guest successfully transitions into a Participant (NTG).

### 🎬 Scene 2: Identity & AI Embeddings - Profile Update
*   **Focus Use Cases:** `Quản lý profile` (QLP), `Cập nhật profile` (CNPP), `Cập nhật AI Vector Embeddings` (UPDATE_EMB).
*   **Actor:** Linh Pham (U4).
*   **Action:**
    1. U4 fills out their profile, adding roles ("Frontend Developer") and skills (React, TypeScript).
    2. U4 saves the profile.
*   **🧠 AI Implicit Highlight:** Explain that `CNPP` triggers a background task via `embedding_service.py`. The system concatenates bio, skills, and goals, passing them through a SentenceTransformer, and upserts a high-dimensional vector into Milvus, making U4 mathematically discoverable.

### 🎬 Scene 3: The "Tough Love" AI - Course Recommendations
*   **Focus Use Cases:** `Xem gợi ý khóa học` (XTG), `Đánh giá thành viên` (DANHGIAD), `Phân tích feedback bằng AI` (PHANTICH).
*   **Actor:** Lan Tran (U8).
*   **Action:**
    1. Log in as U8. Note that U8 has `LATE` checkouts in the seed data (T8, T10).
    2. Navigate to **Skill Development / Suggestions** (`GET /api/v1/suggestions/courses`).
    3. The screen displays targeted course recommendations (e.g., Time Management, Project Planning).
*   **🧠 AI Implicit Highlight:** Explain `suggestion_service.py`. The AI engine reads the `Task` table for `LATE` statuses and reads `ProjectEvaluations`, dynamically boosting recommendation weights for Time Management courses without manual intervention.

### 🎬 Scene 4: The Spark - Swiping & Matching
*   **Focus Use Cases:** `Tạo danh sách gợi ý` (GEN_DECK), `Swipe card` (SWC), `Like` (LIKE), `Tạo match` (TAMH).
*   **Actors:** Minh Nguyen (U1 - Owner) and Thu Le (U2 - Contributor).
*   **Action:**
    1. **U2 Perspective:** Go to the Discover page. Vector similarities surface Minh's project (P4). U2 reviews the card and swipes Right (`LIKE`).
    2. **U1 Perspective:** Switch to U1. Go to Candidates deck. The system surfaces U2 for the "UI/UX Designer" slot. U1 swipes Right (`LIKE`).
    3. **The Match:** The `TAMH` use case triggers, automatically creating a Chat room (`CHAT`).
*   **🧠 AI Implicit Highlight:** Emphasize the `GEN_DECK` phase. Cards are the result of hybrid matching: Milvus vector search (`_search_similar_users`) combined with exact-match rules (Jaccard similarity on skills via `matching_service.py`).

### 🎬 Scene 5: Getting to Work - Task Management
*   **Focus Use Cases:** `Tạo task` (TAT), `Phân công task` (PHANCONG), `Cập nhật trạng thái task` (CAPNHATT), `Báo cáo hoàn thành` (BAOCAOHT), `Đóng task` (DONGT).
*   **Actors:** Minh Nguyen (U1) and Thu Le (U2).
*   **Action:**
    1. U1 goes to Project P4's Kanban, creates a new Task, and assigns it to U2.
    2. Switch to U2. U2 moves the task to "In Progress" and submits work via Checkout (`BAOCAOHT`).
    3. Switch to U1. U1 reviews the submitted work and clicks `Đóng task` (Close), improving U2's reputation score.

### 🎬 Scene 6: Keeping the Peace - Trust & Safety
*   **Focus Use Cases:** `Báo cáo vi phạm` (BCTN), `Quản lý hệ thống` (QLHT), `Xử lý report` (XYR).
*   **Actor:** Admin (U9).
*   **Action:**
    1. Admin (U9) logs into the System Dashboard.
    2. Admin views the Trust & Safety queue (seeded with pending reports/blocks).
    3. Admin reviews a flagged report (`XYR`) and executes an action to ban a user or delete a project (`KTA` / `GDT`).

## Verification
- Run `python backend/reset_database.py --reset all --yes` prior to the demo to confirm seeding succeeds.
- Manually run the Android app and verify login works with seeded accounts (`admin@hibuddy.local` / `HiBuddyDemo!2026`).
- Use Postman or curl to hit `GET /api/v1/suggestions/courses` for user U8 to verify the LATE task recommendation logic fires correctly.