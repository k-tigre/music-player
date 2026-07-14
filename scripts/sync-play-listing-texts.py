#!/usr/bin/env python3
"""Sync Play listing texts from docs/marketing/*/play-listing.md."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

APPS = {
    "music": {
        "source": ROOT / "docs/marketing/music-player/play-listing.md",
        "listings": ROOT / "apps/PlayerApp/src/main/play/listings",
    },
    "book": {
        "source": ROOT / "docs/marketing/audiobook/play-listing.md",
        "listings": ROOT / "apps/AudioBook/src/main/play/listings",
    },
}

FIELDS = ("title", "short-description", "full-description")

# Google Play limits (characters)
LIMITS = {
    "title": 30,
    "short-description": 80,
    "full-description": 4000,
}


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8", newline="\n")


def parse_listing(markdown: str) -> dict[str, dict[str, str]]:
    locales: dict[str, dict[str, str]] = {}
    current_locale: str | None = None

    for line in markdown.splitlines():
        locale_match = re.match(r"^##\s+([\w-]+)\s*$", line)
        if locale_match:
            current_locale = locale_match.group(1)
            locales.setdefault(current_locale, {})
            continue

        field_match = re.match(r"^###\s+([\w-]+)\s*$", line)
        if field_match and current_locale:
            field = field_match.group(1)
            locales[current_locale][field] = ""
            locales[current_locale]["__current__"] = field
            continue

        if current_locale:
            current_field = locales[current_locale].get("__current__")
            if current_field:
                existing = locales[current_locale].get(current_field, "")
                locales[current_locale][current_field] = (
                    f"{existing}\n{line}" if existing else line
                )

    for locale_data in locales.values():
        locale_data.pop("__current__", None)
    return locales


def sync_app(app: str, dry_run: bool = False, check_limits: bool = True) -> int:
    cfg = APPS[app]
    source: Path = cfg["source"]
    listings: Path = cfg["listings"]

    if not source.is_file():
        raise SystemExit(f"Missing source file: {source}")

    locales = parse_listing(read_text(source))
    if not locales:
        raise SystemExit(f"No locales found in {source}")

    changed = 0
    for locale, fields in sorted(locales.items()):
        for field in FIELDS:
            if field not in fields:
                print(f"warning: {app} {locale} missing ### {field}", file=sys.stderr)
                continue
            content = fields[field].strip()
            if check_limits and field in LIMITS:
                limit = LIMITS[field]
                length = len(content)
                if length > limit:
                    print(
                        f"error: {app} {locale} {field} is {length} chars "
                        f"(limit {limit})",
                        file=sys.stderr,
                    )
                    raise SystemExit(1)
            target = listings / locale / f"{field}.txt"
            if target.is_file() and read_text(target).strip() == content:
                continue
            changed += 1
            print(f"{'would write' if dry_run else 'write'} {target}")
            if not dry_run:
                write_text(target, content)
    return changed


def resolve_apps(value: str) -> list[str]:
    if value == "all":
        return list(APPS.keys())
    if value not in APPS:
        raise SystemExit(f"Unknown app '{value}'. Use: music, book, all")
    return [value]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--app",
        choices=("music", "book", "all"),
        default="all",
        help="Which app listing to sync (default: all)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would change without writing",
    )
    parser.add_argument(
        "--no-limits",
        action="store_true",
        help="Skip Google Play character limit checks",
    )
    args = parser.parse_args()

    total = 0
    for app in resolve_apps(args.app):
        print(f"=== {app} ===")
        total += sync_app(app, dry_run=args.dry_run, check_limits=not args.no_limits)
    if args.dry_run:
        print(f"{total} file(s) would change")
    else:
        print(f"{total} file(s) updated")


if __name__ == "__main__":
    main()
