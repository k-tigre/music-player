# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Build Commands
```bash
# Debug build – generates a debuggable APK for local testing
echo "Running debug build"
./gradlew assembleDebug

# QA build (Firebase App Distribution) – includes minification and Firebase upload
echo "Building QA variant"
./gradlew assembleQa

# Release build – requires release keystore credentials set as environment variables
echo "Preparing release build"
./gradlew assembleRelease 

# Run all unit tests across modules
./gradlew test

# Check for dependency updates (Gradle Versions Plugin)
echo "Checking dependencies"
./gradlew dependencyUpdates

# Generate SQLDelight database code from .sq files
echo "Generating SQLDelight interfaces"
./gradlew generateSqlDelightInterface
```

## Build Variants
| Variant     | Debuggable | Minify | App ID Suffix |
|-------------|------------|--------|---------------|
| **debug**   | ✓          | ✗      | `.dev`        |
| **qa**      | ✓          | ✓      | `.dev`        |
| **release** | ✗          | ✓      | _(none)_      |

## Environment Variables (Release Signing)

- **Music Player:**
  - `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD`
  - `MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD`

- **AudioBook:**
  - `AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD`
  - `AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD`

## Project Architecture

Two Android apps built with Kotlin and Jetpack Compose, sharing a common set of modules. Uses a modular multi-project architecture with
manual dependency injection and Decompose for navigation.

- Compile SDK: 34 | Target SDK: 33 | Min SDK: 26 | Java: 17

### App Modules

- **apps:PlayerApp** – Music Player app
- **apps:AudioBook** – Audiobook app (WIP)

### Shared Module Structure

- **core:base:data, core:music:data, core:book:data**
    - `playback` - Media3 ExoPlayer wrapper and playback control
    - `catalog` - Music catalog data source using Android MediaStore
    - `storage:preferences` - SharedPreferences wrapper
    - `storage:database:music` - SQLDelight database for playback queue persistence

- **core:entity**
    - `catalog` - Song, Artist, Album entities
    - `playback` - SongInQueueItem entities
    - `queue` - Queue domain model

- **core:presentation**
    - `catalog` - Catalog browsing (Artists → Albums → Songs)
    - `player` - Full player and mini player views
    - `playlist:queue` - Current queue management UI
    - `background_player` - Background playback service with Media3 MediaSession

- **core:platform**
    - `permission` - Runtime permissions handling

- **tools**
    - `coroutines` - Coroutine scope and extensions
    - `entity` - Optional type wrapper
    - `presentation:compose` - Compose UI utilities (Theme, Colors, Views)
    - `presentation:decompose` - Decompose navigation base classes
    - `platform:utils` - Platform-specific utilities

- **logger** - Multi-backend logging (Logcat, Crashlytics, Internal DB)

- **debug:settings** - Debug-only UI for viewing internal logs

### Key Architectural Patterns
#### Manual Dependency Injection
The app uses manual DI with a simple graph pattern:

- `ApplicationGraph` is the root DI container created in `App.onCreate()`
- Each module defines a `*Dependency` interface and a `*Module` implementation
- Components are created via `*ComponentProvider` interfaces that take dependencies as constructor parameters
- See `ApplicationGraph.create()` in `apps/PlayerApp/src/main/java/by/tigre/music/player/core/di/ApplicationGraph.kt` and
  `apps/AudioBook/src/main/java/by/tigre/audiobook/core/di/ApplicationGraph.kt`

#### Decompose Navigation

Navigation uses Arkivanov Decompose for state‑preserving navigation:

- Components implement interfaces and are created via Providers
- Navigation is handled via `StackNavigation` with sealed config classes
- Components have corresponding `*ViewProvider` for UI rendering
- See `RootCatalogComponent` for an example of nested navigation (Artists → Albums → Songs)

#### BaseComponentContext
All components extend `BaseComponentContext` which combines `ComponentContext` (Decompose lifecycle/state saving) with a
`CoroutineScope`. See
`tools/presentation/decompose/src/main/kotlin/by/tigre/music/player/presentation/base/BaseComponentContext.kt`.

#### View/Component Separation
Each feature follows this pattern:
- `*Component.kt` - Logic layer (interface + Impl class)
- `*View.kt` - UI layer (Compose composable with `.Draw()` extension)
- `*ComponentProvider.kt` - Factory for creating components with DI
- `*ViewProvider.kt` - Factory for creating views
- `navigation/*Navigator.kt` - Navigation interface

#### Playback Architecture
Music playback uses AndroidX Media3 (ExoPlayer):
- `PlaybackController` - Main playback API with queue management
- `PlaybackPlayer` - ExoPlayer wrapper interface
- `PlaybackService` - MediaSessionService for background playback with media controls
- Queue state is persisted via SQLDelight (`Queue.sq`)
- See `core:data:playback` module

#### Logger
- `Log.init()` is called in `App.onCreate()`
- Debug/QA builds: Logcat, Crashlytics, and Internal DB logger
- Release builds: Crashlytics only
- See `logger/core` and `logger/internal-store` modules

### Key File Locations

- App entry point (Music Player): `apps/PlayerApp/src/main/java/by/tigre/music/player/App.kt`
- Main activity (Music Player): `apps/PlayerApp/src/main/java/by/tigre/music/player/MainActivity.kt`
- Root DI graph: `apps/PlayerApp/src/main/java/by/tigre/music/player/core/di/ApplicationGraph.kt` and
  `apps/AudioBook/src/main/java/by/tigre/audiobook/core/di/ApplicationGraph.kt`
- Root navigation component (Music Player): `apps/PlayerApp/src/main/java/by/tigre/music/player/presentation/root/component/Root.kt`
- Dependency versions: `buildSrc/src/main/kotlin/Dependencies.kt`
- App configuration: `buildSrc/src/main/kotlin/Application.kt`
- Module registry: `settings.gradle.kts`

### Key Dependencies

- **Kotlin** 2.1.21 | **AGP** 8.13.2 | **Gradle** 8.9
- **Jetpack Compose** 1.8.0 | **Material3** 1.4.0
- **Decompose** 3.4.0
- **Media3** 1.5.0
- **SQLDelight** 2.2.1
- **Coroutines** 1.10.2
- **Firebase BOM** 34.0.0 (Crashlytics + Analytics)
- **Coil Compose** 3.1.0
