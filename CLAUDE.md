# CLAUDE.md — AI context for this repository

Kotlin monorepo: **3 apps** (2 Android + 1 Desktop) sharing KMP modules. Architecture: **manual DI** (`ApplicationGraph`) + **Decompose** navigation + **Component/View** split. Read this file before making changes.

---

## Quick routing — where to work

| Task | Start here |
|------|------------|
| Android music player shell / root nav | `apps/PlayerApp/src/main/java/by/tigre/music/player/` |
| Android audiobook shell / night timer | `apps/AudioBook/src/main/java/by/tigre/audiobook/` |
| Desktop app shell / windows | `apps/PlayerDesktop/src/main/kotlin/by/tigre/music/player/desktop/` |
| Shared music catalog UI | `core/music/presentation/catalog/` |
| Shared player / equalizer UI | `core/base/presentation/player/` |
| Shared queue UI | `core/music/presentation/playlist/queue/` |
| Audiobook catalog UI | `core/book/presentation/catalog/` |
| Playback engine (ExoPlayer / JVM) | `core/base/data/playback/` |
| Music playback + queue logic | `core/music/data/playback/` |
| Audiobook chapter playback | `core/book/data/playback/` |
| SQLDelight schemas | `core/*/data/storage/database/src/commonMain/sqldelight/` |
| Compose theme / shared widgets | `tools/presentation/compose/` |
| Decompose helpers | `tools/presentation/decompose/` |
| Dependency versions | `buildSrc/src/main/kotlin/Dependencies.kt` |
| App IDs / SDK / versions | `buildSrc/src/main/kotlin/Application.kt` |
| Module registry | `settings.gradle.kts` |

**Rule of thumb:** UI and domain logic go in `core:*` KMP modules; app-specific wiring (DI graph, root nav, Car, night timer) stays in `apps:*`.

---

## Apps

### PlayerApp (Android music player)
- Package: `by.tigre.music.player` | App ID: `by.tigre.musicplayer` (no dot)
- Entry: `apps/PlayerApp/.../App.kt`, `MainActivity.kt`
- DI: `apps/PlayerApp/.../core/di/ApplicationGraph.kt`
- Root nav: `apps/PlayerApp/.../presentation/root/component/Root.kt`
- Car: `apps/PlayerApp/.../car/MusicCarMediaLibrary.kt`
- Playback: `playbackController` (songs from MediaStore)

### AudioBook (Android audiobook)
- Package: `by.tigre.audiobook` | App ID: `by.tigre.audiobook`
- Entry: `apps/AudioBook/.../App.kt`, `MainActivity.kt`
- DI: `apps/AudioBook/.../core/di/ApplicationGraph.kt` — extends PlayerApp graph + book modules + `nightTimerController`
- Root nav: `apps/AudioBook/.../presentation/root/component/Root.kt` — initial screen is **Player**, not catalog
- Playback: `audiobookPlaybackController` (books/chapters); prev/next = ±60s seek, not track skip
- App-only features: night timer (`apps/AudioBook/.../nighttimer/`), face-down extender, audiobook Car library
- Still uses shared music modules for queue infra, preferences, base playback

### PlayerDesktop (JVM Compose Desktop)
- Package: `by.tigre.music.player.desktop`
- Entry: `apps/PlayerDesktop/.../desktop/Main.kt`
- DI: `apps/PlayerDesktop/.../desktop/di/ApplicationGraph.kt` (`DesktopApplicationGraph`)
- Multi-window UI (library, player, equalizer) — not single-activity Android nav
- Data dir: `~/.music-player`
- No Firebase, Car, permissions, `background_player`, or `debug:settings`
- Logger: console only

---

## Module map

```
apps:PlayerApp          — Android music app
apps:AudioBook          — Android audiobook app
apps:PlayerDesktop      — Desktop music app

core:base:data:playback              — ExoPlayer/JVM playback, equalizer, app volume
core:base:presentation:player        — Player, mini-player, equalizer UI
core:base:presentation:background_player — Android MediaSessionService + Car (Android-only)

core:music:entity:catalog|playback   — Song, Artist, Album, SongInQueueItem
core:music:data:catalog              — MediaStore (Android) / folder scan (Desktop)
core:music:data:playback             — Music PlaybackController
core:music:data:storage:database     — SQLDelight queue (DatabaseMusic)
core:music:presentation:catalog      — Artists → Albums → Songs UI
core:music:presentation:playlist:queue — Queue management UI

core:book:entity:catalog             — Book, chapter entities
core:book:data:catalog|playback|storage:database — Audiobook data layer
core:book:presentation:catalog       — Audiobook library UI

core:data:storage:preferences        — SharedPreferences / desktop prefs
core:platform:permission             — Runtime permissions (Android)

tools:presentation:compose|decompose — Shared UI theme + Decompose base
tools:entity|coroutines|platform:utils
debug:settings                       — Debug log viewer (Android debug/qa only)
```

### Dependency layering (do not violate)

```
apps → presentation → data → entity
                    ↘ base:data:playback, tools:*
```

- `entity` modules: pure Kotlin types, no Android/Compose
- `data` modules: repositories, playback, SQLDelight
- `presentation` modules: Component + View + Providers (KMP: commonMain + androidMain + desktopMain)
- `apps` modules: wire graphs, root navigation, platform-only code

---

## Architecture patterns

### Manual DI
1. `ApplicationGraph.create()` in `App.onCreate()` — root container
2. `*Dependency` interface — what a feature needs from the graph
3. `*Module` with `Impl` — data-layer factories
4. `*ComponentProvider.Impl(dependency)` — creates Decompose components

### Decompose navigation
- `StackNavigation` + `@Serializable` sealed config classes
- Components extend `BaseComponentContext` (= `ComponentContext` + `CoroutineScope`)
  - `tools/presentation/decompose/.../BaseComponentContext.kt`
- Child stacks: `appChildStack`, `appChildContext` extensions in same module
- Nested example: `RootCatalogComponent` (Artists → Albums → Songs)

### Component / View split (every feature)

| File | Role |
|------|------|
| `*Component.kt` | Logic: interface + `Impl` |
| `*View.kt` | UI: class implementing `ComposableView` with `.Draw(Modifier)` |
| `*ComponentProvider.kt` | Factory: `interface + Impl(dependency)` |
| `*ViewProvider.kt` | Factory for views |
| `navigation/*Navigator.kt` | Navigation callbacks; implemented in parent component or app `Root` |

Reference implementation — music catalog artist list:
- Component: `core/music/presentation/catalog/.../component/ArtistListComponent.kt`
- View: `.../view/ArtistListView.kt`
- Providers: `.../di/CatalogComponentProvider.kt`, `CatalogViewProvider.kt`
- Navigator: `.../navigation/CatalogNavigator.kt`

### UI state
- `ScreenContentState` + `ScreenContentStateDelegate` for loading/error/empty/content

### Playback
- `PlaybackController` — music queue API (`core:music:data:playback`)
- `PlaybackPlayer` — ExoPlayer wrapper (`core:base:data:playback`)
- `PlaybackService` — background `MediaSessionService` (`core:base:presentation:background_player`)
- Queue persisted via SQLDelight `Queue.sq`
- Equalizer shared across all apps: `EqualizerComponent` in `core:base:presentation:player`

### Strings & resources
- KMP modules: `src/commonMain/composeResources/values/strings.xml` (+ `values-ru/`)
  - Generated access: `Res.string.*` (package set in `compose.resources { packageOfResClass }`)
- Android app strings: `apps/*/src/main/res/values/strings.xml`
- Locales: `en`, `ru` (`androidResources.localeFilters`)

### Logger (external package)
- Artifact: `com.github.k-tigre:logger-*` v1.0.2 from GitHub Packages
- Needs `gpr.user`/`gpr.key` or `GITHUB_ACTOR`/`GITHUB_TOKEN` in Gradle
- Debug/QA Android: Logcat + Crashlytics + Internal DB
- Release Android: Crashlytics only
- Desktop: `ConsoleLogger`

---

## Recipe: add a new screen (Decompose)

1. Add `@Serializable` config to parent's sealed `*Config` class
2. Create `*Component.kt` (interface + Impl extending `BaseComponentContext`)
3. Create `*View.kt` with `ComposableView.Draw()`
4. Add `*Navigator.kt` interface; implement in parent component or app `Root`
5. Add factory methods to `*ComponentProvider` / `*ViewProvider`
6. Register child in parent's `appChildStack` factory
7. If new data needed: add to `*Dependency` interface + wire in `ApplicationGraph`

Put shared UI in `core:*:presentation:*`; put app-specific nav wiring in `apps:*`.

---

## Recipe: add a new Gradle module

1. Add `include(":...")` in `settings.gradle.kts`
2. Add enum entry in `buildSrc/.../Dependencies.kt` → `Project` sealed class
3. Create `build.gradle.kts` following a sibling module (KMP vs Android-only)
4. Wire `implementation(project(...))` from dependent modules
5. KMP modules: `commonMain` + `androidMain` + `jvm("desktop")`, `jvmToolchain(21)`

---

## Stable quirks — do NOT "fix" without a migration plan

| Quirk | Location |
|-------|----------|
| Package `entiry` (not `entity`) | `by.tigre.music.player.core.entiry.*`, `by.tigre.audiobook.core.entiry.*` |
| Package `backgound_player` (not `background`) | Module dir is `background_player/`, but Kotlin package is `...presentation.backgound_player.*` |
| App ID `by.tigre.musicplayer` vs package `by.tigre.music.player` | Intentional mismatch |

---

## Build & verify

Windows: use `gradlew.bat` instead of `./gradlew`.

```bash
# Android — install debug
./gradlew :apps:PlayerApp:installDebug
./gradlew :apps:AudioBook:installDebug

# Desktop — run
./gradlew :apps:PlayerDesktop:run

# Build variants (both Android apps)
./gradlew assembleDebug      # debuggable, no minify, .dev suffix
./gradlew assembleQa         # debuggable, minify, .dev suffix, Firebase distribution
./gradlew assembleRelease    # requires signing env vars

# Tests (no test sources exist yet; command is prepared)
./gradlew test

# After editing .sq files
./gradlew generateSqlDelightInterface

# Dependency updates
./gradlew dependencyUpdates
```

### Build variants

| Variant | Debuggable | Minify | App ID suffix | App name suffix |
|---------|------------|--------|---------------|-----------------|
| debug | yes | no | `.dev` | ` Dev` |
| qa | yes | yes | `.dev` | ` Qa` |
| release | no | yes | none | none |

### Signing env vars

- Music Player: `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD`, `MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD`
- AudioBook: `AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD`, `AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD`

---

## Platform & versions

| Setting | Value |
|---------|-------|
| Kotlin | 2.1.21 |
| AGP | 8.13.2 |
| Gradle | 8.9 |
| JVM toolchain | 21 |
| Compile SDK | 36 |
| Target SDK | 35 |
| Min SDK | 26 |
| Compose (Android) | 1.8.0 |
| Compose Multiplatform | 1.8.1 |
| Material3 | 1.4.0 |
| Decompose | 3.4.0 |
| Media3 | 1.5.0 |
| SQLDelight | 2.2.1 |
| Coroutines | 1.10.2 |
| Firebase BOM | 34.0.0 |
| Coil | 3.1.0 |

App versions: Music Player `0.17.0`, AudioBook `0.2.0`, Desktop `1.0.5`.

---

## Coding constraints for AI agents

1. **Minimize scope** — match existing patterns in the nearest sibling file; no drive-by refactors
2. **Respect module boundaries** — don't import `apps:*` from `core:*`; don't put Android APIs in `entity`
3. **Both apps may need wiring** — if changing shared `core:*`, check PlayerApp AND AudioBook `ApplicationGraph` / `Root`
4. **KMP source sets** — platform code in `androidMain`/`desktopMain`, shared in `commonMain`
5. **No tests exist** — don't claim test coverage; run relevant `assembleDebug` or `:apps:PlayerDesktop:run` to verify
6. **Don't rename `entiry`/`backgound_player`** — breaks imports across the entire codebase
7. **Commits/PRs** — only when explicitly asked by the user
