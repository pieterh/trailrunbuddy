# Trail Run Buddy — Implementation Plan

## Context

Trail Run Buddy is a greenfield Android app that helps trail runners set up timer profiles with configurable alerts (drink, eat, stretch) that fire during a run — even with the screen off. The app is offline-only, uses Room for persistence, and must survive process death mid-session. The project structure should allow future KMP extraction but only implements Android now.

**Source files:** `AGENT.md` (stack/rules), `USECASES.md` (requirements), `input files/` (mockups + audio)

---

## 1. Project Structure

```
com.trailrunbuddy.app/
├── TrailRunBuddyApp.kt              # @HiltAndroidApp
├── MainActivity.kt                   # Single Activity, @AndroidEntryPoint
│
├── core/                             # Platform-agnostic utilities
│   ├── di/AppModule.kt               # Hilt: DB, DAOs, dispatchers
│   ├── di/RepositoryModule.kt        # Hilt: repo bindings
│   └── util/TimeFormatter.kt         # HH:MM:SS formatting
│   └── util/ColorGenerator.kt        # Deterministic color from name
│
├── data/                             # Data layer
│   ├── local/
│   │   ├── TrailRunBuddyDatabase.kt
│   │   ├── dao/{ProfileDao, TimerDao, SessionDao}.kt
│   │   ├── entity/{ProfileEntity, TimerEntity, SessionEntity}.kt
│   │   └── relation/ProfileWithTimers.kt
│   ├── repository/{ProfileRepositoryImpl, SessionRepositoryImpl, SettingsRepositoryImpl}.kt
│   └── datastore/SettingsDataStore.kt  # DataStore for theme pref
│
├── domain/                           # Pure Kotlin (future KMP shared)
│   ├── model/{Profile, Timer, Session, SessionState, TimerType, ThemeMode}.kt
│   ├── repository/{ProfileRepository, SessionRepository, SettingsRepository}.kt  # Interfaces
│   └── usecase/
│       ├── profile/{GetProfiles, GetProfileWithTimers, SaveProfile, DeleteProfile, UndoDeleteProfile}UseCase.kt
│       ├── timer/{AddTimer, UpdateTimer, DeleteTimer}UseCase.kt
│       ├── session/{StartSession, PauseSession, ResumeSession, StopSession, GetActiveSession}UseCase.kt
│       └── settings/{GetTheme, SetTheme}UseCase.kt
│
├── platform/                         # Android-specific
│   ├── audio/AudioManager.kt         # SoundPool wrapper, USAGE_ALARM
│   ├── service/SessionService.kt     # Foreground service, START_STICKY
│   ├── service/SessionServiceConnection.kt  # Binder bridge to ViewModels
│   ├── notification/SessionNotificationManager.kt
│   └── timer/SessionTimerEngine.kt   # Wall-clock based countdown engine
│
└── ui/
    ├── navigation/{NavGraph, Screen}.kt
    ├── theme/{Theme, Color, Type, Shape}.kt
    ├── components/{ProfileAvatar, TimerCard, ConfirmationDialog, CountdownDisplay, ErrorText}.kt
    ├── profilelist/{Screen, ViewModel, UiState, UiEvent}.kt
    ├── profiledetail/{Screen, ViewModel, UiState, UiEvent}.kt
    ├── activesession/{Screen, ViewModel, UiState, UiEvent}.kt
    └── settings/{Screen, ViewModel, UiState, UiEvent}.kt
```

---

## 2. Data Model

### Room Entities

**ProfileEntity** — `profiles` table: `id` (PK auto), `name`, `color_hex`, `created_at`

**TimerEntity** — `timers` table: `id` (PK auto), `profile_id` (FK → profiles, CASCADE delete), `name`, `duration_seconds`, `timer_type` ("REPEATING"/"ONCE"), `sort_order`

**SessionEntity** — `sessions` table: single row (PK = 1), `profile_id`, `state`, `started_at`, `paused_at`, `total_paused_ms`, `timer_states_json` (per-timer cycle count + fired-once flag)

**ProfileWithTimers** — Room `@Relation` joining profile + its timers

### Domain Models
Pure Kotlin data classes mirroring entities without Room annotations: `Profile`, `Timer`, `Session`, `TimerType` enum, `SessionState` enum, `ThemeMode` enum.

---

## 3. Implementation Phases

### Phase 1: Project Scaffolding
- Create Android project with `build.gradle.kts` — all deps pinned per AGENT.md
- `TrailRunBuddyApp`, `MainActivity`, M3 theme files
- Placeholder navigation graph with empty screens
- `AndroidManifest.xml`: FOREGROUND_SERVICE, POST_NOTIFICATIONS, WAKE_LOCK permissions

### Phase 2: Data Layer
- Room entities, DAOs, database, type converters
- `SettingsDataStore` for theme (DataStore, not Room)
- DAO instrumented tests

### Phase 3: Domain Layer
- Domain models, repository interfaces, repository implementations with entity↔domain mapping
- Hilt modules (`AppModule`, `RepositoryModule`)
- Use cases with validation:
  - `SaveProfileUseCase`: blank name → error, no timers → error
  - `DeleteProfileUseCase`: active session → blocked; caches for undo
  - `AddTimerUseCase`: 0-second duration → error
- Unit tests for all use cases

### Phase 4: Profile List Screen
- `ProfileListScreen`: LazyColumn with SwipeToDismiss, ProfileAvatar (2-char initials in colored circle), FAB for new profile, snackbar with undo
- `ProfileListViewModel`: observes profiles + active session, manages delete/undo
- Cannot delete profile with active session (PM-11)

### Phase 5: Profile Detail Screen
- `ProfileDetailScreen`: editable name, timer list with duration/name/type badge/edit+delete, add-timer dialog (name, hours, minutes, repeating/once toggle)
- `ProfileDetailViewModel`: validates on save, manages in-memory timer list for new profiles
- Matches mockup: profile header card + "TIMERS" section

### Phase 6: Timer Engine & Audio
- `SessionTimerEngine`: wall-clock drift correction via `System.currentTimeMillis() - startedAt - totalPausedMs`, emits countdown states + PRE_WARNING (T-10s) / ALERT (T-0) events
- `AudioManager`: `SoundPool` with `USAGE_ALARM` attribute, preloads `alert.wav` + `warning_beep.wav`, 4 max streams for concurrent playback
- Unit tests with controlled clock

### Phase 7: Foreground Service
- `SessionService`: `START_STICKY`, partial WakeLock, persists session state to Room periodically
  - Fresh start: loads profile, creates SessionEntity, starts engine
  - Restart after kill: reads SessionEntity, reconstructs position from wall clock (RS-2)
- `SessionServiceConnection`: singleton bridge exposing `StateFlow<List<TimerCountdownState>>` and `StateFlow<SessionState>` to ViewModels
- `SessionNotificationManager`: live countdown of next timer, "Stop" action, tap opens ActiveSession

### Phase 8: Active Session Screen
- `ActiveSessionScreen`: timer countdown cards (name, HH:MM:SS, cycle count, progress), pause/resume + stop controls, BackHandler with confirmation dialog
- `ActiveSessionViewModel`: collects from `SessionServiceConnection`, handles stop confirmation flow

### Phase 9: Settings Screen
- Theme toggle: Dark / Light / System Default (radio buttons)
- `SettingsViewModel` reads/writes via `SettingsDataStore`
- `MainActivity` observes theme flow and applies to M3 theme

### Phase 10: Navigation & Integration
- `Screen` sealed class: ProfileList, ProfileDetail(id), ActiveSession(id), Settings
- Start destination: ProfileList unless active session exists → ActiveSession (RS-2, NI-2)
- Notification PendingIntent opens ActiveSession with profileId
- ActiveSession back/stop always shows confirmation dialog

### Phase 11: Polish & Edge Cases
- Notification permission request (Android 13+)
- WakeLock acquire/release lifecycle
- Memory leak audit (service connections, SoundPool)

### Phase 12: Testing
- **Unit** (JUnit 4 + MockK + Turbine): all use cases, all ViewModels, timer engine, utilities
- **Instrumented** (Espresso + Compose test): all DAOs, all screens, full navigation flow

---

## 4. Key Architecture Decisions

### Service ↔ UI Communication
```
ProfileListVM / ActiveSessionVM
         ↕ StateFlow
SessionServiceConnection (singleton)
         ↕ bind/intent
SessionService (foreground, START_STICKY)
    ├── SessionTimerEngine (wall-clock math)
    ├── AudioManager (SoundPool, USAGE_ALARM)
    └── Room (SessionEntity — crash recovery)
```

### Wall-Clock Drift Correction (SL-6)
```
elapsedMs = System.currentTimeMillis() - startedAt - totalPausedMs
remainingMs = durationMs - (elapsedMs % durationMs)   // repeating
cycleCount  = (elapsedMs / durationMs).toInt()         // repeating
```
Every tick recalculates from wall clock — immune to drift.

### Undo Delete
`DeleteProfileUseCase` caches `ProfileWithTimers` in memory before Room delete. `UndoDeleteProfileUseCase` re-inserts cached data. Cache cleared on next delete or timeout.

### Future KMP Readiness
- `core/` + `domain/` = pure Kotlin, no Android imports → extractable to shared module
- `data/` repository interfaces in domain; Room impls swappable for SQLDelight
- `platform/` = explicitly Android-only (Service, SoundPool, notifications)
- `ui/` = Compose for Android; SwiftUI + Liquid Glass for iOS

---

## 5. Verification Plan

1. **Build**: Project compiles without errors after each phase
2. **Unit tests**: `./gradlew test` — all use cases, ViewModels, engine tests pass
3. **Instrumented tests**: `./gradlew connectedAndroidTest` — DAO + UI tests pass
4. **Manual smoke test**:
   - Create profile → add 2 timers (one repeating, one once) → save → verify in list
   - Start session → hear pre-warning beep at T-10s → hear alert at T-0 → verify cycle count increments
   - Pause → countdowns freeze → Resume → countdowns continue
   - Put phone in silent mode → alerts still audible
   - Lock screen → alerts still fire
   - Force-kill app → reopen → session restored at correct position
   - Stop session → confirm dialog → returns to list
   - Swipe-delete profile → snackbar → undo → profile restored
   - Settings → toggle theme → UI updates
