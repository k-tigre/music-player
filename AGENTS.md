# AGENTS.md

In-repo context for AI agents. Process and product notes live in the **knowledge** MCP
(`knowledge_search` / `knowledge_get` / `knowledge_list`).
Scopes: `process` + `domains/music-player`. Architecture deltas vs the KMP baseline:
`domains/music-player/architecture.md`. Prefer writing lasting facts into the private knowledge repo.

## Quick facts

- Kotlin monorepo (`rootProject.name = TigreMedia`) — 3 apps (`PlayerApp`, `AudioBook`, `PlayerDesktop`) + shared KMP `core:*` / `tools:*`
- Pattern: manual DI (`ApplicationGraph`) + Decompose + Component/View split
- Shared UI and domain → `core:*`; app wiring (DI, root nav, Car, night timer, paywall) → `apps:*`
- Each Android app has its **own** `ApplicationGraph` (AudioBook does not subclass PlayerApp)
- Stable typos: package `entiry`, dir and package `backgound_player` — do not rename
- App id `by.tigre.musicplayer` vs package `by.tigre.music.player` is intentional
- App versions: `buildSrc/src/main/kotlin/Application.kt`. Library versions: `buildSrc/src/main/kotlin/Dependencies.kt`. Do not copy them
  into this file
- JDK / `jvmToolchain`: **21**. On Windows use `gradlew.bat`
- Verify: `./gradlew :apps:PlayerApp:assembleDebug`, `:apps:AudioBook:assembleDebug`, or `:apps:PlayerDesktop:run`
- Before claiming done, asking for a manual check, or offering a commit: run the `qa-impact` skill against `qa-impact/checks.yaml`

## Where to work

| Task                                  | Start here                                                          |
|---------------------------------------|---------------------------------------------------------------------|
| Android music shell / root nav        | `apps/PlayerApp/src/main/java/by/tigre/music/player/`               |
| Android audiobook shell / night timer | `apps/AudioBook/src/main/java/by/tigre/audiobook/`                  |
| Desktop shell / windows               | `apps/PlayerDesktop/src/main/kotlin/by/tigre/music/player/desktop/` |
| Shared music catalog UI               | `core/music/presentation/catalog/`                                  |
| Queue / playlist library UI           | `core/music/presentation/playlist/`                                 |
| Favorites UI                          | `core/music/presentation/favorites/`                                |
| Shared player / equalizer UI          | `core/base/presentation/player/`                                    |
| Audiobook catalog UI                  | `core/book/presentation/catalog/`                                   |
| Playback engine (ExoPlayer / desktop) | `core/base/data/playback/`                                          |
| Music playback + queue logic          | `core/music/data/playback/`                                         |
| Audiobook chapter playback            | `core/book/data/playback/`                                          |
| Billing / entitlements                | `core/platform/billing/`, `core/platform/entitlements/`             |
| SQLDelight schemas                    | `core/*/data/storage/database/src/commonMain/sqldelight/`           |
| Compose theme / shared widgets        | `tools/presentation/compose/`                                       |
| Decompose helpers                     | `tools/presentation/decompose/`                                     |
| Analytics                             | `tools/analytics/`                                                  |
| Module registry                       | `settings.gradle.kts`                                               |
| Play listing texts / screenshots      | `.cursor/skills/prepare-play-listing/SKILL.md`                      |

## Apps

### PlayerApp (Android music)

- Package `by.tigre.music.player`, app id `by.tigre.musicplayer`
- Entry: `App.kt`, `MainActivity.kt`. DI: `core/di/ApplicationGraph.kt`. Root nav: `presentation/root/component/Root.kt`
- Catalog from MediaStore. Car: `car/MusicCarMediaLibrary.kt`. Playback: `playbackController`

### AudioBook (Android audiobook)

- Package and app id `by.tigre.audiobook`
- Root initial screen is **Player**, not the catalog
- Playback: `audiobookPlaybackController`. Remote prev/next is **seek ±60s**, not track skip
- App-only: night timer (`nighttimer/`), face-down extender, audiobook Car library
- Reuses shared music modules for queue infra, preferences, and base playback

### PlayerDesktop (JVM Compose Desktop)

- Package `by.tigre.music.player.desktop`. DI: `DesktopApplicationGraph`
- Multi-window UI (library, player, equalizer). Data dir: `~/.music-player`
- No Firebase, Car, runtime permissions, `background_player`, or `debug:settings`. Logger: console only
- Billing is a no-op; entitlements are the free repository (features allowed)

## Layers

```
apps → presentation → data → entity
                    ↘ base:data:playback, platform:*, tools:*
```

- `entity`: pure Kotlin types, no Android or Compose
- `data`: repositories, playback, SQLDelight
- `presentation`: Component + View + Providers (KMP: `commonMain` + `androidMain` + `desktopMain`)
- `apps`: graphs, root navigation, platform-only code

Do not import `apps:*` from `core:*`. Do not put Android APIs in `entity`.

## Patterns

### Manual DI

1. `ApplicationGraph.create()` in `App.onCreate()`
2. `*Dependency` — what a feature needs from the graph
3. `*Module` with `Impl` — data-layer factories
4. `*ComponentProvider.Impl(dependency)` — creates Decompose components

### Decompose

- `StackNavigation` + `@Serializable` sealed config classes
- Components extend `BaseComponentContext` (`tools/presentation/decompose/`)
- Child stacks: `appChildStack` / `appChildContext` in that module
- Nested example: `RootCatalogComponent` (Artists → Albums → Songs)

### Component / View (every feature)

| File                                         | Role                                                         |
|----------------------------------------------|--------------------------------------------------------------|
| `*Component.kt`                              | Logic: interface + `Impl`                                    |
| `*View.kt`                                   | UI: `ComposableView` with `.Draw(Modifier)`                  |
| `*ComponentProvider.kt` / `*ViewProvider.kt` | Factories                                                    |
| `navigation/*Navigator.kt`                   | Callbacks; implemented in the parent component or app `Root` |

Reference: `ArtistListComponent` / `ArtistListView` under `core/music/presentation/catalog/`. List state uses `ScreenContentState` +
`ScreenContentStateDelegate`.

Shared player UI state is `EqualizerComponent` in `core:base:presentation:player`. Background playback is `PlaybackService` (
`MediaSessionService`) in `core:base:presentation:background_player`. Music queue persists via SQLDelight `Queue.sq`.

### New screen

1. Add a `@Serializable` config to the parent's sealed `*Config`
2. Create `*Component.kt` (`Impl` extends `BaseComponentContext`) and `*View.kt` (`ComposableView.Draw()`)
3. Add `*Navigator.kt`; implement it in the parent or app `Root`
4. Add factories on `*ComponentProvider` / `*ViewProvider` and register the child in the parent's `appChildStack`
5. New data: extend `*Dependency` and wire it in `ApplicationGraph`

Shared UI goes in `core:*:presentation:*`. App-specific nav stays in `apps:*`. A shared `core:*` change often needs wiring in **both**
Android graphs.

### New Gradle module

1. `include(":...")` in `settings.gradle.kts`
2. Enum entry in `buildSrc/.../Dependencies.kt` → `Project`
3. `build.gradle.kts` copied from a sibling (KMP vs Android-only)
4. `implementation(project(...))` from dependents
5. KMP: `commonMain` + `androidMain` + `jvm("desktop")`, `jvmToolchain(21)`

KMP Android uses `com.android.kotlin.multiplatform.library` (`kotlin { android { ... } }`), not `com.android.library` + `androidTarget()`.
Apps and Android-only libraries use AGP built-in Kotlin (no `org.jetbrains.kotlin.android`); keep Compose, Parcelize, and Serialization
plugins where they are already applied.

### Strings

KMP: `src/commonMain/composeResources/values/strings.xml` and `values-ru/`. Access: `Res.string.*`. Android app strings:
`apps/*/src/main/res/values/strings.xml`. Locales: `en`, `ru`.

### Logger

Artifact `com.github.k-tigre:logger-*` from GitHub Packages. Gradle needs `gpr.user` / `gpr.key` or `GITHUB_ACTOR` / `GITHUB_TOKEN`.
Debug/QA Android: Logcat + Crashlytics + internal DB. Release Android: Crashlytics only. Desktop: `ConsoleLogger`.

## Build

```bash
./gradlew :apps:PlayerApp:installDebug
./gradlew :apps:AudioBook:installDebug
./gradlew :apps:PlayerDesktop:run
./gradlew test
./gradlew generateSqlDelightInterface   # after .sq edits
```

| Variant | Debuggable | Minify | App ID suffix | Name suffix |
|---------|------------|--------|---------------|-------------|
| debug   | yes        | no     | `.dev`        | ` Dev`      |
| qa      | yes        | yes    | `.dev`        | ` Qa`       |
| release | no         | yes    | none          | none        |

Release signing env vars: `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD` / `MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD`, and
`AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD` / `AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD`.

`:tools:analytics:checkAnalyticsDocs` fails when `docs/analytics-events.md` is out of sync with `@AnalyticsScope`. Regenerate with
`:tools:analytics:generateAnalyticsDocs` and commit that file (it is the tracked exception under ignored `docs/`).

## Constraints

1. Match the nearest sibling. No drive-by refactors.
2. Respect module boundaries (see Layers).
3. Platform code in `androidMain` / `desktopMain`; shared code in `commonMain`.
4. Do not rename `entiry` or `backgound_player`.
5. Release tags are only `v.m.X.Y.Z` / `v.b.X.Y.Z`. An app release uploads the AAB; it does not update the Play listing.
6. Commits and PRs only when the user asks. Do not commit ignored `docs/` or secrets. Exception: regenerated `docs/analytics-events.md`.

## Releases and Play listing

| Task | Skill / entry |
|------|----------------|
| Store texts, screenshots, feature graphic | [.cursor/skills/prepare-play-listing/SKILL.md](.cursor/skills/prepare-play-listing/SKILL.md) |
| Version bump / Play release notes | `scripts/prepare-release.ps1` |

App release tags (`v.m.*` / `v.b.*`) upload the AAB only. Listing updates use Actions → **Publish Play listing**.
