#!/usr/bin/env bash
set -euo pipefail

APP="${1:-}"
case "$APP" in
  music|book|all) ;;
  *)
    echo "Usage: $0 music|book|all" >&2
    exit 1
    ;;
esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${ANDROID_PUBLISHER_CREDENTIALS:-}" ]]; then
  echo "Set ANDROID_PUBLISHER_CREDENTIALS (Play service account JSON)." >&2
  exit 1
fi
if [[ -z "${PLAY_CONTACT_EMAIL:-}" ]]; then
  echo "Set PLAY_CONTACT_EMAIL (Play Console developer contact)." >&2
  exit 1
fi

publish() {
  local module="$1"
  ./gradlew ":${module}:publishPlayListing"
}

case "$APP" in
  music) publish "apps:PlayerApp" ;;
  book) publish "apps:AudioBook" ;;
  all)
    publish "apps:PlayerApp"
    publish "apps:AudioBook"
    ;;
esac
