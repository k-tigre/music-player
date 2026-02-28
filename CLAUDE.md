# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# QA build (Firebase App Distribution)
./gradlew assembleQa

# Release build (requires release keystore credentials in environment)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Check for dependency updates
./gradlew dependencyUpdates

# Generate SQLDelight database code from .sq files
./gradlew :core:data:storage:database:music:generateSqlDelightInterface
```

### Build Variants

| Variant | Debuggable | Minify | App ID Suffix |
|---------|-----------|--------|---------------|
| **debug** | ✓ | ✗ | `.dev` |
| **qa** | ✓ | ✓ | `.dev` |
| **release** | ✗ | ✓ | _(none)_ |

### Environment Variables (Release Signing)

**Music Player:**
- `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD`
- `MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD`

**AudioBook:**
- `AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD`
- `AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD`

## Project Architecture

Two Android apps built with Kotlin and Jetpack Compose, sharing a common set of modules. Uses a modular multi-project architecture with manual dependency injection and Decompose for navigation.

- Compile SDK: 34 | Target SDK: 33 | Min SDK: 26 | Java: 17
- K2 compiler is enabled (`kotlin.experimental.tryK2=true` in `gradle.properties`)

### App Modules

- **androidApp** - Music Player app (v0.14.1)
- **AudioBook** - AudioBook app (v0.1.0, `poc/book` branch) — currently a copy of the Music Player sharing all core modules; intended template for audiobook-specific features

### Shared Module Structure

- **core:data**
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
- See `ApplicationGraph.create()` in `androidApp/src/main/java/by/tigre/music/player/core/di/ApplicationGraph.kt`

#### Decompose Navigation

Navigation uses Arkivanov Decompose for state-preserving navigation:

- Components implement interfaces and are created via Providers
- Navigation is handled via `StackNavigation` with sealed config classes
- Components have corresponding `*ViewProvider` for UI rendering
- See `RootCatalogComponent` for an example of nested navigation (Artists → Albums → Songs)

#### BaseComponentContext

All components extend `BaseComponentContext` which combines `ComponentContext` (Decompose lifecycle/state saving) with a `CoroutineScope`. See `tools/presentation/decompose/src/main/kotlin/by/tigre/music/player/presentation/base/BaseComponentContext.kt`.

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

- App entry point: `androidApp/src/main/java/by/tigre/music/player/App.kt`
- Main activity: `androidApp/src/main/java/by/tigre/music/player/MainActivity.kt`
- Root DI graph: `androidApp/src/main/java/by/tigre/music/player/core/di/ApplicationGraph.kt`
- Root navigation component: `androidApp/src/main/java/by/tigre/music/player/presentation/root/component/Root.kt`
- Dependency versions: `buildSrc/src/main/kotlin/Dependencies.kt`
- App configuration: `buildSrc/src/main/kotlin/Application.kt`
- Module registry: `settings.gradle.kts`

### Key Dependencies

- **Kotlin** 1.9.24 | **AGP** 8.3.2 | **Gradle** 8.9
- **Jetpack Compose** 1.6.8 | **Material3** 1.2.1
- **Decompose** 2.2.3
- **Media3** 1.3.1
- **SQLDelight** 2.0.2
- **Coroutines** 1.8.1
- **Firebase BOM** 32.7.2 (Crashlytics + Analytics)
- **Coil** 2.7.0
