# AGENTS.md

AI agents: read **[CLAUDE.md](CLAUDE.md)** for full project context.

Quick facts:
- Kotlin monorepo — 3 apps (`PlayerApp`, `AudioBook`, `PlayerDesktop`) + shared KMP `core:*` modules
- Pattern: manual DI (`ApplicationGraph`) + Decompose + Component/View split
- Shared code → `core:*`; app wiring → `apps:*`
- Stable typos: package `entiry`, dir `backgound_player` — do not rename
- Verify: `./gradlew :apps:PlayerApp:assembleDebug` or `./gradlew :apps:PlayerDesktop:run`
