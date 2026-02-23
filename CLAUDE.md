# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Build Variants

```bash
# Debug build
./gradlew assembleDebug

# QA build
./gradlew assembleQa

# Release build (requires release keystore credentials in environment)
./gradlew assembleRelease
```

### Dependency Management

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Check for dependency updates in specific module
./gradlew :module:dependencyUpdates
```

### Generate SQLDelight Code

```bash
# Generate database code from .sq files
./gradlew :core:data:storage:database:music:generateSqlDelightInterface
```

## Project Architecture

This is an Android music player app built with Kotlin and Jetpack Compose, using a modular multi-project architecture with manual dependency
injection.

### Module Structure

The project follows a layered architecture with modules organized by purpose:

- **androidApp** - Main application module containing MainActivity and app initialization
- **core:data** - Data layer modules
    - `playback` - Media3 Exoplayer wrapper and playback control
    - `catalog` - Music catalog data source using Android MediaStore
    - `storage:preferences` - SharedPreferences wrapper
    - `storage:database:music` - SQLDelight database for playback queue persistence
- **core:entity** - Domain entities
    - `catalog` - Song, Artist, Album entities
    - `playback` - SongInQueueItem, Queue entities
- **core:presentation** - UI layer components
    - `catalog` - Catalog browsing (Artists -> Albums -> Songs)
    - `player` - Full player and mini player views
    - `playlist:queue` - Current queue management
    - `background_player` - Background playback service with Media3 MediaSession
- **tools** - Shared utilities
    - `coroutines` - Coroutine scope and extensions
    - `entity` - Optional type wrapper
    - `presentation:compose` - Compose UI utilities (Theme, Colors, Views)
    - `presentation:decompose` - Decompose navigation base classes
    - `platform:utils` - Platform-specific utilities
- **logger** - Multi-backend logging system (Logcat, Crashlytics, Internal DB)
- **debug:settings** - Debug-only UI for viewing logs (only in debug builds)

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
- See `RootCatalogComponent` for an example of nested navigation (Artists -> Albums -> Songs)

#### BaseComponentContext

All components extend `BaseComponentContext` which combines:

- `ComponentContext` from Decompose (for lifecycle, state saving)
- `CoroutineScope` (for coroutines)
- See `tools/presentation/decompose/src/main/kotlin/by/tigre/music/player/presentation/base/BaseComponentContext.kt`

#### Playback Architecture

Music playback uses AndroidX Media3 (ExoPlayer):

- `PlaybackController` - Main playback API with queue management
- `PlaybackPlayer` - ExoPlayer wrapper interface
- `PlaybackService` - MediaSessionService for background playback with media controls
- Queue state is persisted via SQLDelight database (`Queue.sq`)
- See `core:data:playback` module

#### View/Component Separation

Each feature follows this pattern:

- `*Component.kt` - Logic layer (interface + Impl class)
- `*View.kt` - UI layer (Compose composable with .Draw() extension)
- `*ComponentProvider.kt` - Factory for creating components with DI
- `*ViewProvider.kt` - Factory for creating views
- `navigation/*Navigator.kt` - Navigation interface

#### Logger

Multi-backend logging system:

- `Log.init()` is called in `App.onCreate()`
- Debug builds: Logcat, Crashlytics, and Internal DB logger
- Release builds: Crashlytics only
- See `logger/core` and `logger/internal-store` modules

### Build Variants

- **debug** - Development build with debug signing
- **qa** - Same as debug but allows testing via Firebase App Distribution
- **release** - Production build with ProGuard/R8 minification

### Environment Variables (for Release)

- `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD` - Release keystore store password
- `MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD` - Release keystore key password

### Key Dependencies

- **Kotlin** 1.9.24
- **Jetpack Compose** 1.6.8 with Material3 1.2.1
- **Decompose** 2.2.3 for navigation
- **Media3** 1.3.1 for playback
- **SQLDelight** 2.0.2 for database
- **Coroutines** 1.8.1

### Important File Locations

- App entry point: `androidApp/src/main/java/by/tigre/music/player/App.kt`
- Main activity: `androidApp/src/main/java/by/tigre/music/player/MainActivity.kt`
- Root DI graph: `androidApp/src/main/java/by/tigre/music/player/core/di/ApplicationGraph.kt`
- Root navigation component: `androidApp/src/main/java/by/tigre/music/player/presentation/root/component/Root.kt`
- Dependency definitions: `buildSrc/src/main/kotlin/Dependencies.kt`
- App configuration: `buildSrc/src/main/kotlin/Application.kt`
