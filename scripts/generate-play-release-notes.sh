#!/usr/bin/env bash
set -euo pipefail

APP="${1:-}"
TRACK="${2:-}"
TAG="${GITHUB_REF_NAME:-}"
TAG="${TAG#refs/tags/}"

VERSION="${RELEASE_VERSION:-}"

if [[ -z "$VERSION" && -n "$TAG" ]]; then
    if [[ "$TAG" =~ ^v\.m\.([0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
        APP="music"
        VERSION="${BASH_REMATCH[1]}"
    elif [[ "$TAG" =~ ^v\.b\.([0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
        APP="book"
        VERSION="${BASH_REMATCH[1]}"
    fi
fi

if [[ -z "$APP" ]]; then
    echo "Usage: $0 <music|book> [track]" >&2
    echo "Or set GITHUB_REF_NAME to v.m.X.Y.Z / v.b.X.Y.Z" >&2
    exit 1
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Invalid release version: '${VERSION}' (expected X.Y.Z or tag v.m.X.Y.Z / v.b.X.Y.Z)" >&2
    exit 1
fi

ARGS=(scripts/changelog_tool.py write-play-notes --app "$APP" --version "$VERSION")
if [[ -n "$TRACK" ]]; then
    ARGS+=(--track "$TRACK")
fi
python "${ARGS[@]}"
