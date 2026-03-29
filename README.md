# Music Player

A modular music player with a catalog backed by the on-device media library, a playback queue, and background playback on **Android** (
Media3 / ExoPlayer), plus a separate **desktop** app built with Compose Multiplatform (JVM). The repo also includes an **AudioBook** Android
app (work in progress) that shares the same core modules.

## Author

**Tigre** — [github.com/k-tigre](https://github.com/k-tigre)

## About

- **Music (PlayerApp)** — main Android player: catalog browsing (artists → albums → tracks), current queue, full-screen and mini player,
  background playback via `MediaSession`.
- **PlayerDesktop** — desktop client in Kotlin and Jetpack Compose for desktop OSes (native installers: MSI, DMG, DEB).
- **AudioBook** — separate Android app for audiobooks (WIP), same overall architecture as the music player.

Stack: Kotlin, Jetpack Compose, Decompose (navigation), SQLDelight (queue), Coil, Firebase Crashlytics (mobile builds). Developer-oriented
details are in [`CLAUDE.md`](CLAUDE.md).

## Requirements

- **JDK 17**
- **Android:** Android SDK (easiest via [Android Studio](https://developer.android.com/studio)); for device install, enable USB debugging or
  use an emulator.
- **Desktop:** a JVM on the developer machine; for packaging a given OS, run the build on that OS (or use CI with the right environment—see
  Gradle tasks below).

## Running per platform

Run these from the repository root. On Windows, use `gradlew.bat` instead of `./gradlew`.

### Android — Music Player (`:apps:PlayerApp`)

```bash
./gradlew :apps:PlayerApp:installDebug
```

Build APK without installing:

```bash
./gradlew :apps:PlayerApp:assembleDebug
```

**Release** builds need signing environment variables (see [`CLAUDE.md`](CLAUDE.md)): `MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD`,
`MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD`.

### Android — AudioBook (`:apps:AudioBook`)

```bash
./gradlew :apps:AudioBook:installDebug
```

For release: `AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD`, `AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD`.

### Desktop — JVM (`:apps:PlayerDesktop`)

Run via Gradle (typical for development):

```bash
./gradlew :apps:PlayerDesktop:run
```

Package a distribution for the **current** OS:

```bash
./gradlew :apps:PlayerDesktop:packageDistributionForCurrentOS
```

Individual formats (run on the target system or in CI with the matching environment): `packageMsi`, `packageDmg`, `packageDeb`, and
`packageUberJarForCurrentOS` for a JAR.

## Tests and checks

```bash
./gradlew test
```

Regenerate SQLDelight interfaces after changing `.sq` files:

```bash
./gradlew generateSqlDelightInterface
```

## Contributing

1. **Fork and branch** — create a branch off `main` with a clear name (`fix/…`, `feature/…`).
2. **Changes** — keep the PR focused; match existing style (naming, DI, Component / View split like other features).
3. **Verification** — ensure `./gradlew test` passes; for touched apps, build and install a debug build when relevant.
4. **PR description** — briefly state the problem and fix; screenshots help for UI changes.
5. **Issues** — open an issue first for larger ideas or bugs if you want to align on direction.

Pull requests and discussion: [k-tigre/music-player](https://github.com/k-tigre/music-player).
