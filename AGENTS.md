# HiBuddy Project DOX

## Purpose
This is the root DOX for the HiBuddy Android application. It defines the global rules, workflows, and responsibilities for the project. HiBuddy is an Android Jetpack Compose app written in Kotlin that connects project contributors with project owners.

## Ownership
- Project Owner: @elaine-tran-24

## Work Guidance
- Use Jetpack Compose for all UI.
- Use MVI-like architecture with ViewModels and Kotlin Coroutines/Flows.
- Avoid committing directly to `main`; use feature branches and PRs.
- `duongupdate-merged` is the latest consolidated branch incorporating all recent features.

## Verification
- Code must compile without errors using `./gradlew assembleDebug`.
- Follow Kotlin standard style guidelines.

## Child DOX Index
- `app/src/main/java/com/example/hibuddy/data/AGENTS.md` - Data Layer (API, Repositories, DTOs, Local Storage)
- `app/src/main/java/com/example/hibuddy/ui/AGENTS.md` - UI Layer (Screens, Components, Theme, ViewModels)
