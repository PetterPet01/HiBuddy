# Feature List 04: Android Client Architecture and Offline Cache

This document expands upon Study Guide 04 by providing an exhaustive list of features supported by the Android mobile client application.

## 1. Jetpack Compose UI & State Management
*   **Declarative Views:** Entire UI constructed using modern Compose functions.
*   **State Hoisting:** Decoupling UI from business logic using `ViewModel` and `StateFlow`.
*   **Navigation Graphs:** Single-Activity architecture using `NavHost` for seamless screen transitions.
*   **Custom Theming:** Implementations of Material 3 design systems (Typography, Colors, Shapes).
*   **Swiping Mechanics:** Custom gesture detection (`Modifier.pointerInput`) mapping drag offsets to Like/Pass logic.

## 2. Architecture & Dependency Injection
*   **MVVM Pattern:** Strict separation of Model, View, and ViewModel.
*   **Service Locator / Custom DI:** Manual dependency injection managing lifecycles of Repositories, APIs, and Databases (`ServiceLocator.kt`).
*   **Repository Pattern:** Serving as the single source of truth, mediating between remote APIs and local storage.

## 3. Network & Authentication Hooks
*   **Retrofit Integration:** Type-safe HTTP client definitions for all REST endpoints.
*   **Auth Interceptors:** Injecting valid JWT access tokens into request headers automatically.
*   **Token Rotation (Authenticator):** Catching `401 Unauthorized` responses and silently requesting new token pairs via the refresh token endpoint before retrying the original request.

## 4. Offline-First Resilience & Local Storage
*   **Room SQLite Integration:** Local database mapping for entities like Chat Messages, Profiles, and Match history.
*   **Flow/LiveData Observation:** ViewModels observing Room databases; UI updates automatically when network data syncs to local storage.
*   **DataSync Workers:** Background tasks reconciling queued offline actions (e.g., pending chat messages) with the server upon reconnection.
*   **Encrypted Preferences:** Secure storage for active JWT tokens and sensitive user settings.
