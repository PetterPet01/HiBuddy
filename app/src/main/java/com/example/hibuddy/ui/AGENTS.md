# UI Layer DOX

## Purpose
Defines the structure and styling guidelines for the UI layer using Jetpack Compose.

## Ownership
- App UI components, screens, and viewmodels.

## Work Guidance
- Use Material 3 design principles and standard `HiBuddyColors` defined in `theme/Theme.kt`.
- All screens must be stateless where possible or manage state via a ViewModel.
- ViewModels should use `StateFlow` for `uiState`.
- React to global state changes (like user switching) by observing flows from the data layer in ViewModel `init` blocks.
- Maintain separate folders for complex screens (e.g., `screens/profile/`, `screens/projects/`).

## Verification
- UI must compile successfully and render without crashes.
- Dialogs must handle back navigation appropriately.
