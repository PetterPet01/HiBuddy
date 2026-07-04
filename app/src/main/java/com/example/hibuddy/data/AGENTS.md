# Data Layer DOX

## Purpose
Defines the responsibilities and structure of the data layer in HiBuddy, including API communication, data models, repositories, and local persistence.

## Ownership
- App Data Layer

## Work Guidance
- Use Retrofit for API communication (`remote/ApiService.kt`).
- Define Data Transfer Objects (DTOs) in `remote/dto/Dtos.kt`.
- Use `runCatching` with the standard `apiResult` wrapper for consistent error handling.
- Store authentication tokens and current user state in `local/TokenManager.kt` via DataStore.
- Use `StateFlow` to expose reactive state (e.g. `currentUserId`).

## Verification
- Ensure all repository methods return robust `Result` types.
- DTOs must align with backend schema changes.
