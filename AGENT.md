# Project description
- We are building and maintaining a multiplatform mobile application called "Trail Run Buddy".
- It will be used to help plan a trail run and gives support during the trail run.
- The list of use cases that the application should support are available in the usecases.md file.

# Project Stack

- Kotlin only
- Jetpack Compose (no XML)
- MVVM architecture
- Hilt for DI
- Retrofit for networking
- Room for database
- Coroutines + Flow only (no LiveData)
- Espresso for user interface testing
- JUnit 4 / MockK for unit testing

---

# Package & Versions
- Package: `com.trailrunbuddy.app`
- Kotlin 2.1.20 / AGP 8.7.3 / Compose BOM 2024.12.01
- KSP 2.1.20-1.0.29 / Hilt 2.52 / Room 2.7.0 / Lifecycle 2.9.0 / Coroutines 1.9.0
- compileSdk/targetSdk 35 / minSdk 31

---

# Architecture Rules

- UI layer contains composables only
- No business logic in composables
- ViewModels handle state and logic
- Repositories manage data sources
- Domain layer contains use cases
- Strict separation between layers
- Keep platform specific code out of the business logic layer

---

# Compose Guidelines

- Prefer stateless composables
- Apply state hoisting
- UI state must be immutable
- Use collectAsStateWithLifecycle for Flow

---

# Performance Rules

- Use LazyColumn for large lists
- Avoid unnecessary recompositions
- Avoid blocking main thread
- Use Dispatchers.IO for data operations

---

# Naming Conventions

- Screens → Screen
- UI state → UiState
- Events → UiEvent
- Repository → Repository

---

# Forbidden

- No LiveData
- No direct database access in ViewModel
- No XML layouts
- No Java files

---

# Multiplatform
- In the future multiplatform (Android and iOS) should be supported
- Create the project in a structure so that other platforms can easily be added
- Implement only for Android

---

# UI
- Use for Android the "Material Design 3" design
- Use for iOS the "Liquid Glass" design

