# Findings

## Discover Screen / Profile Mode Mismatch
- **Issue**: The `DiscoverScreen` currently allows any user to toggle between "OWNER" (browsing contributors) and "CONTRIBUTOR" (browsing projects) using the `DiscoverHeader`. It does not enforce the `selectedMode` (i.e., "OWNER", "CONTRIBUTOR", or "BOTH") that the user selected during the Profile Wizard (`CompleteProfileScreen`).
- **Root Cause**: `DiscoverViewModel` relies on `ServiceLocator.discoverMode` for its initial state and toggles it freely. It doesn't fetch or respect the authenticated user's `profile.mode` from the data layer (`ProfileRepository`).
- **Proposed Fix**: 
  1. Update `DiscoverUiState` to include `val profileMode: String? = null`.
  2. In `DiscoverViewModel`, fetch the current user's profile on initialization using `ServiceLocator.profileRepository.getMyProfile()`.
  3. Once fetched, if the user's mode is strictly "OWNER" or "CONTRIBUTOR", force `ServiceLocator.discoverMode` and `uiState.mode` to match that mode.
  4. In `DiscoverScreen`'s `DiscoverHeader`, conditionally display the mode toggle button *only* if `uiState.profileMode == "BOTH"`. If the user has a strict role, hide the toggle button so they are locked into their chosen experience.

## Task Management (Phase 3)
- Checked `TasksScreen.kt`. File attachments in `TaskEditDialog` and `TaskSubmissionDialog` correctly use `ActivityResultContracts.GetContent()` to read `ByteArray` payloads and upload them via `ServiceLocator.taskRepository.uploadTaskAttachment()`.

## UI Polish & Navigation (Phase 4)
- **Clickable Cards**: Verified that `Modifier.clickable { onOpenUser(card.userId) }` and `Modifier.clickable { onOpenProject(card.projectId) }` are properly implemented in the Discover swipe cards.
- **Empty States**: Verified `EmptyStackView` shows correct messaging based on the current mode ("You've seen everyone!" vs "No approved recruiting projects right now").
- **User Details**: Verified `ProjectsTab` and `FeedbackTab` are fully implemented in `UserDetailScreen.kt`.
- **Date Picker**: Verified `DatePickerField` uses Material 3 `DatePickerDialog` and correctly forwards the formatted date string.
- **Chat Invitations**: Verified `ChatScreen.kt` implements the `ProjectInvitationPanel` and `InviteProjectDialog`.
