# Feature List 05: Admin, Moderation, and Trust Systems

This document expands upon Study Guide 05 by providing an exhaustive list of features supporting trust, safety, and administrative workflows.

## 1. Flagging, Reporting, and Moderation
*   **User Reporting System:** Ability for users to flag profiles or projects with specific categories and comments.
*   **Automatic Threshold Triggers:** Moving entities to `FLAGGED_PENDING_REVIEW` upon receiving a configurable number of reports.
*   **Discovery Exclusion:** Real-time filtering preventing flagged projects/users from appearing in swipe decks or search results.
*   **Match Suspension:** Temporarily disabling chat and interactions on matches involving flagged entities.
*   **Admin Dashboard Integration:** Centralized portal (`/admin`) for moderators to review active reports.
*   **Resolution Workflows:** Admin capabilities to approve (clear flags) or ban/delete offensive entities.
*   **Cascading Cleanup:** Automatic removal of associated vector embeddings, active chats, and pending matches upon an entity ban.
*   **User Blocking:** Implementing `UserBlock` to permanently prevent matching and visibility between specific individuals.

## 2. Academic Student Email Verification
*   **Domain Validation:** Restricting registration or specific features to recognized institutional email extensions.
*   **OTP Generation and Delivery:** Issuing 6-digit numeric verification codes via secure SMTP.
*   **Time-To-Live (TTL) Enforcement:** Rejecting OTPs after a strict 10-minute expiration window.
*   **One-Time Consumption:** Discarding OTPs immediately upon successful use to prevent replay.
*   **Rate Limiting and Backoff:** Blocking IPs/emails after successive failed verification attempts to deter brute-force attacks.
*   **Feature Gating:** Restricting core actions (swiping, creating groups) until `verified_student` status is confirmed.
