---
name: prepare-play-listing
description: >-
  Updates Google Play listing texts from docs, rebuilds marketing screenshots,
  and publishes listing to Play (separate from app releases). Use when the user
  asks to update Play store description, listing texts, screenshots, feature
  graphic, or says «обнови листинг», «пересобери скриншоты для Play»,
  «залей листинг в Play».
---

# Prepare Play Listing

Обновление описаний и скриншотов Google Play — **отдельно** от релиза приложения.

Два приложения: **music** (`PlayerApp`) и **book** (`AudioBook`). Локали: `en-US`, `ru-RU`.

## Что делает

1. Копирует тексты из `docs/marketing/{music-player|audiobook}/play-listing.md` в `apps/*/src/main/play/listings/`
2. Снимает скриншоты Roborazzi и собирает креативы (`recordMarketingScreenshots`)
3. Копирует PNG в `play/listings/*/graphics/` и `docs/marketing/*/assets/output/`

## Когда запускать

- Изменились тексты листинга (название, краткое/полное описание)
- Изменился UI — нужны новые скриншоты
- Обновили подписи в `docs/marketing/*/assets/config.json`

**Не нужно** при каждом patch-релизе — только когда меняется маркетинг.

## Шаг 1. Отредактируйте тексты (если нужно)

| App | Texts | Captions |
|-----|-------|----------|
| music | [docs/marketing/music-player/play-listing.md](../../docs/marketing/music-player/play-listing.md) | [config.json](../../docs/marketing/music-player/assets/config.json) |
| book | [docs/marketing/audiobook/play-listing.md](../../docs/marketing/audiobook/play-listing.md) | [config.json](../../docs/marketing/audiobook/assets/config.json) |

Секции `## ru-RU` / `## en-US`, поля `### title`, `### short-description`, `### full-description`.

Лимиты Google Play: title 30, short 80, full 4000 символов.

## Шаг 2. Пересоберите ассеты

```powershell
.\scripts\prepare-play-listing.ps1 -App music   # или book | all
```

Linux / Git Bash:

```bash
chmod +x scripts/prepare-play-listing.sh
./scripts/prepare-play-listing.sh music   # или book | all
```

Только тексты (без пересъёмки скриншотов):

```powershell
python scripts/sync-play-listing-texts.py --app all
# или
.\scripts\prepare-play-listing.ps1 -App all -TextsOnly
```

Только скриншоты одного приложения:

```powershell
.\gradlew.bat :apps:PlayerApp:buildMarketingScreenshots
.\gradlew.bat :apps:AudioBook:buildMarketingScreenshots
```

## Шаг 3. Проверка

```powershell
python scripts/sync-play-listing-texts.py --app all --dry-run
git diff apps/PlayerApp/src/main/play/listings/ apps/AudioBook/src/main/play/listings/ docs/marketing/
```

Откройте несколько PNG в `docs/marketing/music-player/assets/output/screenshots/ru/` и `docs/marketing/audiobook/assets/output/screenshots/ru/`.

## Шаг 4. Закоммитьте

Коммитьте **не** `docs/` (локальные исходники и кэш ассетов), а результат в приложениях и скрипты:

```powershell
git add apps/PlayerApp/src/main/play/listings/
git add apps/AudioBook/src/main/play/listings/
git add scripts/sync-play-listing-texts.py scripts/prepare-play-listing.* scripts/publish-play-listing.*
# при правках пайплайна: .cursor/skills/prepare-play-listing/ .github/workflows/publish_play_listing.yml
git commit -m "Update Play listing texts and screenshots"
git push
```

`docs/marketing/*/play-listing.md` и `docs/marketing/*/assets/` остаются локальными (в `.gitignore`).
## Шаг 5. Опубликуйте листинг в Play

Отдельно от релиза приложения. Релизный CI (**Release** / tag `v.m.*` / `v.b.*`) **не** трогает описания и скриншоты.

Нужна GitHub Variable `PLAY_CONTACT_EMAIL` (и опционально `PLAY_CONTACT_WEBSITE`).
Secret `ANDROID_PUBLISHER_CREDENTIALS` уже используется для релизов.

**GitHub Actions** (рекомендуется): Actions → **Publish Play listing** → Run workflow → выбрать `music` / `book` / `all`.

**Локально**:

```powershell
$env:PLAY_CONTACT_EMAIL = "you@example.com"
.\scripts\publish-play-listing.ps1 -App music   # или book | all
```

## Зависимости (локально)

```powershell
pip install pillow
```

Подробнее о скриншотах: [docs/marketing/screenshot-build.md](../../docs/marketing/screenshot-build.md).

## Связанные файлы

- [docs/marketing/music-player/play-listing.md](../../docs/marketing/music-player/play-listing.md)
- [docs/marketing/audiobook/play-listing.md](../../docs/marketing/audiobook/play-listing.md)
- [scripts/sync-play-listing-texts.py](../../scripts/sync-play-listing-texts.py)
- [scripts/prepare-play-listing.ps1](../../scripts/prepare-play-listing.ps1)
- [scripts/publish-play-listing.ps1](../../scripts/publish-play-listing.ps1)
- [.github/workflows/publish_play_listing.yml](../../.github/workflows/publish_play_listing.yml)
- `apps/PlayerApp/build.gradle.kts` / `apps/AudioBook/build.gradle.kts` — `publishPlayListing`, `buildMarketingScreenshots`
