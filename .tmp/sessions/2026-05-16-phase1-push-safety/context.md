# Task Context: Phase 1 - Push Notifications and Trust/Safety

Session ID: 2026-05-16-phase1-push-safety
Created: 2026-05-16
Status: in_progress

## Current Request
Implement Phase 1 of the app refinement: Push Notifications (Firebase Cloud Messaging) for matches/messages, and Trust & Safety features (Report/Block users) which is mandatory for App Store approval.

## Context Files (Standards to Follow)
(No specific context files discovered, following general FastAPI and Kotlin/Compose best practices)

## Reference Files (Source Material to Look At)
- backend/app/models/
- backend/app/api/
- app/build.gradle.kts
- app/src/main/java/com/example/hibuddy/ui/screens/

## External Docs Fetched
None yet. Will fetch Firebase FCM and related docs if needed.

## Components
1. Backend: Database models for User Blocks and Reports.
2. Backend: API endpoints for blocking and reporting.
3. Backend: FCM integration for sending push notifications (New Match, New Message).
4. Android: FCM client implementation (Service to receive pushes).
5. Android: UI for Reporting/Blocking users from Profile or Chat screens.

## Constraints
- Must maintain the existing async SQLAlchemy architecture.
- Android UI must use Jetpack Compose.
- Ensure proper error handling and clean UI states.

## Exit Criteria
- [ ] Users can block and report other users via the API and Android UI.
- [ ] Backend is configured to trigger push notifications.
- [ ] Android app is configured to receive and display push notifications.
