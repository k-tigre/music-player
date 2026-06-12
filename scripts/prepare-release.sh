#!/usr/bin/env bash
set -euo pipefail

APP="${1:-}"
VERSION="${2:-}"
if [[ -z "$APP" || -z "$VERSION" ]]; then
    echo "Usage: $0 <music|book> X.Y.Z" >&2
    exit 1
fi
if [[ "$APP" != "music" && "$APP" != "book" ]]; then
    echo "Invalid app: '${APP}' (expected music or book)" >&2
    exit 1
fi
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Invalid version: '${VERSION}' (expected X.Y.Z)" >&2
    exit 1
fi
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 scripts/changelog_tool.py bump-version --app "$APP" --version "$VERSION"
python3 scripts/changelog_tool.py write-play-notes --app "$APP" --version "$VERSION"
python3 scripts/changelog_tool.py finalize --app "$APP" --version "$VERSION"
