#!/usr/bin/env bash
set -euo pipefail

APP="${1:-all}"
TEXTS_ONLY="${2:-}"

case "$APP" in
  music|book|all) ;;
  *)
    echo "Usage: $0 [music|book|all] [--texts-only]" >&2
    exit 1
    ;;
esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PYTHON=python3
command -v python3 >/dev/null 2>&1 || PYTHON=python

"$PYTHON" scripts/sync-play-listing-texts.py --app "$APP"

if [[ "$TEXTS_ONLY" == "--texts-only" ]]; then
  echo "Listing texts updated (screenshots skipped)."
  exit 0
fi

run_marketing() {
  local module="$1"
  ./gradlew ":${module}:recordMarketingScreenshots"
}

case "$APP" in
  music) run_marketing "apps:PlayerApp" ;;
  book) run_marketing "apps:AudioBook" ;;
  all)
    run_marketing "apps:PlayerApp"
    run_marketing "apps:AudioBook"
    ;;
esac

echo "Play listing assets updated under apps/*/src/main/play/listings/"
