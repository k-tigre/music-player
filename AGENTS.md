# AGENTS.md

AI agents: read **[CLAUDE.md](CLAUDE.md)** for full project context.

Process/domain knowledge: use the **knowledge** MCP
(`knowledge_search` / `knowledge_get` / `knowledge_list`).
Scopes: `process` + `domains/music-player`. Prefer writing lasting facts into the private knowledge repo, not inventing process details.

Quick facts:
- Kotlin monorepo — 3 apps (`PlayerApp`, `AudioBook`, `PlayerDesktop`) + shared KMP `core:*` modules
- Pattern: manual DI (`ApplicationGraph`) + Decompose + Component/View split
- Shared code → `core:*`; app wiring → `apps:*`
- Stable typos: package `entiry`, dir `backgound_player` — do not rename
- Verify: `./gradlew :apps:PlayerApp:assembleDebug` or `./gradlew :apps:PlayerDesktop:run`

## Releases and Play listing

| Task | Skill / entry |
|------|----------------|
| Store texts, screenshots, feature graphic | [.cursor/skills/prepare-play-listing/SKILL.md](.cursor/skills/prepare-play-listing/SKILL.md) |
| Version bump / Play release notes | `scripts/prepare-release.ps1` |

App release tags (`v.m.*` / `v.b.*`) upload the AAB only. Listing updates use Actions → **Publish Play listing**.
