# Study Guide 04: Android Client Architecture and Offline Synchronization

The HiBuddy Android application is a native, modern, responsive app written in **Kotlin**. It is engineered to be offline-first, highly responsive, and modular. This module analyzes the MVVM design, Jetpack Compose navigation paradigms, and the underlying network synchronization layer.

---

## 🏛️ 1. Jetpack Compose & Single Activity MVVM Architecture

HiBuddy is built on a modern Android architecture framework utilizing Jetpack Compose, Kotlin Coroutines, and ViewModel structures.

### Architectural Core
*   **Single-Activity Architecture**: The entire application runs inside a single parent entry point: `MainActivity.kt`. It implements Jetpack Compose Navigation via a centralized `NavHost` containing defined `Routes` (e.g., `auth/login`, `main/discover`).
*   **Model-View-ViewModel (MVVM)**:
    *   **View (Jetpack Compose Screens)**: UI components are entirely declarative, utilizing Material 3 components. They subscribe to state emissions and expose UI events (such as clicks) up to the ViewModels.
    *   **ViewModel**: Manages the screen's UI State as a Kotlin `StateFlow` (read-only state wrapper). ViewModels launch operations within the safe scope of Kotlin Coroutines (`viewModelScope`) and trigger data operations on background threads via the repositories.
    *   **Model**: Mapped cleanly through local database entities (Room) and JSON-serializable remote data transfer objects (DTOs).
*   **State Hoisting**: UI elements do not hold independent state. They receive standard, immutable state classes from the ViewModel and bubble interaction events upward, preventing UI synchronization inconsistencies.

---

## 🛠️ 2. Dependency Injection via Service Locator

Instead of bringing in heavy reflection-based dependency injection frameworks like Hilt or Dagger (which can slow down compilation times and add steep learning curves), HiBuddy implements a highly efficient, explicit **Service Locator** pattern inside `/app/src/main/java/com/example/hibuddy/ServiceLocator.kt`.

### Implementation Principles
*   **Singleton Pool**: The `ServiceLocator` is an explicit Kotlin `object` initialized once on application launch (`HiBuddyApplication.kt`).
*   **Lazy Initialization**: Services, databases, and repositories are declared using Kotlin's `by lazy` syntax. This ensures objects (such as the SQLite Room Database) are only compiled and instantiated when they are first accessed by a ViewModel, accelerating cold-start times.
*   **Dependencies Configured**:
    *   *Core Client*: `OkHttpClient` with custom logging and header interceptors.
    *   *Retrofit*: Parsed using Gson, configured with a unified endpoint registry (`ApiService.kt`).
    *   *Storage*: Room local database instance (`HiBuddyDatabase`) and the SharedPreferences-based `TokenManager`.
    *   *Repositories*: Singletons representing `AuthRepository`, `ProfileRepository`, `ProjectRepository`, `SwipeRepository`, and `ChatRepository`.

---

## 🌐 3. Resilient Network Layer & Auto-Token Rotation

The communication layer must handle unstable cellular connections and silently manage security sessions without interrupting the user. This is solved by combining **OkHttp Interceptors** and **Authenticators**.

### Interceptor & Authenticator Orchestration Flow

```
[ Retrofit Outgoing Request ]
            │
            ▼
┌──────────────────────────────────────┐
│  AuthInterceptor                     │
│  - Reads accessToken from Storage    │
│  - Adds "Authorization: Bearer <tok>"│
└──────────────────────────────────────┘
            │
            ▼
[ Transmit Over Internet ] ──────► [ Server API (FastAPI) ]
            │                                 │
[ Receive Response ] ◄────────────────────────┘
            │
      Is Status 401?
     ├── No  ──► [ Return Data to App / ViewModel ]
     └── Yes
            ▼
┌──────────────────────────────────────┐
│  AuthAuthenticator                  │
│  - Blocks concurrent threads         │
│  - Calls /api/auth/refresh (Sync)    │
│  - Updates Storage with new Tokens   │
│  - Retries original request          │
└──────────────────────────────────────┘
```

1.  **Authorization Header Injection (`AuthInterceptor`)**: Every outgoing HTTP request passes through this interceptor. It checks if an access token is stored in SharedPreferences via `TokenManager`. If present, it automatically appends the token to the header as `Authorization: Bearer <token>`.
2.  **Handling Token Expirations (`AuthAuthenticator`)**: If the backend rejects the token (returns `401 Unauthorized`), the OkHttp framework triggers the custom `AuthAuthenticator`.
3.  **Synchronous Refresh Rotation**:
    *   The authenticator intercepts the `401` response.
    *   It retrieves the stored `refresh_token`.
    *   It makes a **synchronous** Retrofit network call to `/api/auth/refresh`.
    *   If the backend returns a new pair of tokens, the authenticator saves them, rebuilds the failed request with the *new* access token, and retries the request transparently.
    *   If the refresh token itself has expired or is invalid, the authenticator clears the local token storage and broadcasts an authorization failure intent, forcing the client app to redirect the user to the login screen.

---

## 💾 4. Offline-First Synchronization via Room Local Database

To guarantee that chat messages and project data are accessible even when offline, HiBuddy implements a local-first caching database using **Room SQLite** (implemented across `ChatLocalDataSource.kt` and `Repositories.kt` under `ChatRepository`).

### Caching and Sync Policies
*   **Local Cache as Single Source of Truth**: When a user opens a chat screen, the UI subscribes directly to a Room SQL query returning a Kotlin `Flow<List<MessageEntity>>`. This flow automatically triggers UI recompositions whenever the underlying database table updates.
*   **Background Sync Pipeline**:
    *   When the phone is online, the `WebSocketManager` listens for incoming payloads.
    *   Upon receiving a new message, the data layer writes the message record to Room.
    *   Room automatically emits the new message on the Flow, updating the UI instantly.
    *   If a message is typed while offline, it is written to Room with a status of `DeliveryState.Sending`. A background job is registered with Android's `WorkManager` to retry sending the pending messages as soon as internet connection is restored.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "What is the difference between an OkHttp Interceptor and an OkHttp Authenticator? Why use both?"
*   **Theoretical Answer**: An **Interceptor** is a pre-request mechanism. It runs on *every single request* to modify outgoing parameters (like adding headers) or log responses. An **Authenticator** is a post-request reactive mechanism. It is *only triggered when the server returns a 401 Unauthorized* response. If you try to refresh tokens inside an Interceptor, you would have to manually parse every response code, which creates redundant code. Furthermore, an Authenticator is highly optimized: it blocks subsequent network threads during a token refresh, prevents redundant multiple token-refresh calls from firing in parallel (handling race conditions), and retries the request automatically.
*   **Code Reference**: Refer to the implementation in `/app/src/main/java/com/example/hibuddy/data/remote/AuthInterceptor.kt` and `AuthAuthenticator.kt`. Show how the Interceptor handles the proactive addition of headers, while the Authenticator handles the reactive refresh workflow.

### Q2: "How does the Room local database and Kotlin Flow ensure that the UI is always up to date?"
*   **Theoretical Answer**: This is achieved through **Observable Queries**. When a Room DAO (Data Access Object) function returns a Kotlin `Flow`, Room automatically registers a database table tracker. Whenever an insert, update, or delete transaction occurs on that table, the Room library is notified. It queries the updated records on a background thread and emits the new dataset through the `Flow` stream. Because the UI ViewModel subscribes to this `Flow` (collecting it as a StateFlow inside the Composable), the Jetpack Compose compiler detects the state change and automatically re-renders (recomposes) the specific UI components with the new data.
*   **Code Reference**: Open the repository file `/app/src/main/java/com/example/hibuddy/data/repository/Repositories.kt` (under `ChatRepository`). Show the lecturer how message histories are exposed using `Flow` structures and updated under the hood whenever a WebSocket event writes data to the Room data source.

---

*Move on to the next section: **[05. Admin, Moderation, & Trust Systems](05_admin_moderation_and_safety.md)** to learn how the trust and safety layers are implemented.*
