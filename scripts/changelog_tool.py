#!/usr/bin/env python3
"""Parse and update per-app CHANGELOG files for Play Store release notes."""
from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

ROOT: Path = Path(__file__).resolve().parents[1]
MAX_LENGTH: int = 500
DEFAULT_RELEASE_NOTES: dict[str, str] = {
    "ru-RU": "Правка багов и улучшения",
    "en-US": "Bug fixes and improvements",
}
VERSION_HEADER: re.Pattern[str] = re.compile(
    r"^## \[(?P<version>[^\]]+)\](?:\s*-\s*(?P<date>\d{4}-\d{2}-\d{2}))?\s*$",
    re.MULTILINE,
)
LOCALE_HEADER: re.Pattern[str] = re.compile(r"^### (RU|EN)\s*$", re.MULTILINE)

APP_CONFIG: dict[str, dict[str, str]] = {
    "music": {
        "changelog": "CHANGELOG-music.md",
        "object_name": "MusicPlayer",
        "release_notes_dir": "apps/PlayerApp/src/main/play/release-notes",
        "play_track": "internal",
        "tag_prefix": "v.m.",
    },
    "book": {
        "changelog": "CHANGELOG-book.md",
        "object_name": "AudioBook",
        "release_notes_dir": "apps/AudioBook/src/main/play/release-notes",
        "play_track": "internal",
        "tag_prefix": "v.b.",
    },
}


def app_config(app: str) -> dict[str, str]:
    if app not in APP_CONFIG:
        raise ValueError(f"Unknown app: '{app}' (expected: music, book)")
    return APP_CONFIG[app]


def changelog_path(app: str) -> Path:
    return ROOT / app_config(app)["changelog"]


def read_changelog(app: str) -> str:
    path: Path = changelog_path(app)
    if not path.is_file():
        raise FileNotFoundError(f"Missing {path}")
    return path.read_text(encoding="utf-8")


def write_changelog(app: str, text: str) -> None:
    changelog_path(app).write_text(text, encoding="utf-8")


def split_sections(text: str) -> list[tuple[str, str, str | None]]:
    matches: list[re.Match[str]] = list(VERSION_HEADER.finditer(text))
    sections: list[tuple[str, str, str | None]] = []
    for index, match in enumerate(matches):
        start: int = match.end()
        end: int = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        sections.append((match.group("version"), match.group("date"), text[start:end]))
    return sections


def find_section(text: str, version: str) -> tuple[str, str | None, str] | None:
    for section_version, section_date, body in split_sections(text):
        if section_version == version:
            return section_version, section_date, body
    return None


def default_release_notes(version: str, reason: str) -> dict[str, str]:
    print(f"Version [{version}] {reason}, using default release notes")
    return dict(DEFAULT_RELEASE_NOTES)


def extract_locale(body: str, locale: str) -> str:
    lines: list[str] = body.splitlines()
    collecting: bool = False
    collected: list[str] = []
    for line in lines:
        locale_match: re.Match[str] | None = LOCALE_HEADER.match(line)
        if locale_match is not None:
            collecting = locale_match.group(1) == locale
            continue
        if line.startswith("## "):
            break
        if collecting:
            stripped: str = line.strip()
            if not stripped:
                continue
            if stripped.startswith("- "):
                collected.append(f"• {stripped[2:].strip()}")
            else:
                collected.append(stripped)
    return "\n".join(collected).strip()


def trim_notes(notes: str) -> str:
    if len(notes) <= MAX_LENGTH:
        return notes
    return notes[: MAX_LENGTH - 1] + "…"


def extract_release_notes(app: str, version: str) -> dict[str, str]:
    text: str = read_changelog(app)
    section: tuple[str, str | None, str] | None = find_section(text, version)
    if section is None:
        return default_release_notes(version, f"not found in {app_config(app)['changelog']}")
    _, _, body = section
    ru_notes: str = extract_locale(body, "RU")
    en_notes: str = extract_locale(body, "EN")
    if not ru_notes and not en_notes:
        return default_release_notes(version, "has empty RU and EN release notes in CHANGELOG")
    if not ru_notes:
        ru_notes = en_notes
    if not en_notes:
        en_notes = ru_notes
    return {
        "ru-RU": trim_notes(ru_notes),
        "en-US": trim_notes(en_notes),
    }


def bump_application_version(app: str, version: str) -> None:
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError(f"Invalid version: '{version}' (expected X.Y.Z)")
    config: dict[str, str] = app_config(app)
    object_name: str = config["object_name"]
    major, minor, patch = version.split(".")
    application_file: Path = ROOT / "buildSrc" / "src" / "main" / "kotlin" / "Application.kt"
    text: str = application_file.read_text(encoding="utf-8")
    pattern: re.Pattern[str] = re.compile(
        rf"(object {re.escape(object_name)} \{{.*?val version: Version = )Version\(\d+, \d+, \d+\)",
        re.DOTALL,
    )
    updated, count = pattern.subn(
        rf"\1Version({major}, {minor}, {patch})",
        text,
        count=1,
    )
    if count != 1:
        raise ValueError(f"Failed to update {object_name}.version in {application_file}")
    application_file.write_text(updated, encoding="utf-8")
    print(f"Bumped {object_name} version to {major}.{minor}.{patch}")


def finalize_version(app: str, version: str, release_date: str | None = None) -> bool:
    text: str = read_changelog(app)
    section: tuple[str, str | None, str] | None = find_section(text, version)
    if section is None:
        print(f"Version [{version}] not found in {app_config(app)['changelog']}, skipping finalize")
        return False
    _, section_date, _ = section
    if section_date:
        print(f"Version [{version}] already has release date {section_date}")
        return False
    dated_header: str = f"## [{version}] - {release_date or date.today().isoformat()}"

    def replacer(match: re.Match[str]) -> str:
        if match.group("version") != version:
            return match.group(0)
        return dated_header

    updated: str = VERSION_HEADER.sub(replacer, text)
    if updated == text:
        raise ValueError(f"Failed to finalize version [{version}] in {app_config(app)['changelog']}")
    write_changelog(app, updated)
    return True


def write_play_release_notes(app: str, version: str, track: str | None, output_dir: Path | None) -> None:
    config: dict[str, str] = app_config(app)
    resolved_track: str = track or config["play_track"]
    resolved_output: Path = output_dir or (ROOT / config["release_notes_dir"])
    notes_by_locale: dict[str, str] = extract_release_notes(app, version)
    for locale, notes in notes_by_locale.items():
        locale_dir: Path = resolved_output / locale
        locale_dir.mkdir(parents=True, exist_ok=True)
        (locale_dir / f"{resolved_track}.txt").write_text(notes + "\n", encoding="utf-8")
        (locale_dir / "default.txt").write_text(notes + "\n", encoding="utf-8")
        print(f"Wrote {resolved_output.name}/{locale}/{resolved_track}.txt ({len(notes)} chars)")


def parse_app_from_tag(tag: str) -> tuple[str, str]:
    for app, config in APP_CONFIG.items():
        prefix: str = config["tag_prefix"]
        if tag.startswith(prefix):
            version: str = tag[len(prefix) :]
            if re.fullmatch(r"\d+\.\d+\.\d+", version):
                return app, version
    raise ValueError(f"Unsupported release tag: '{tag}' (expected v.m.X.Y.Z or v.b.X.Y.Z)")


def add_app_arg(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--app",
        choices=sorted(APP_CONFIG),
        required=True,
        help="music = Music Player, book = AudioBook",
    )


def main() -> int:
    parser: argparse.ArgumentParser = argparse.ArgumentParser(description="CHANGELOG helper for Play releases")
    subparsers = parser.add_subparsers(dest="command", required=True)

    extract_parser = subparsers.add_parser("extract", help="Print release notes for a version")
    add_app_arg(extract_parser)
    extract_parser.add_argument("--version", required=True)
    extract_parser.add_argument("--locale", choices=["ru-RU", "en-US"], required=True)

    bump_parser = subparsers.add_parser("bump-version", help="Update Application.kt version for one app")
    add_app_arg(bump_parser)
    bump_parser.add_argument("--version", required=True)

    finalize_parser = subparsers.add_parser("finalize", help="Add release date to a version section")
    add_app_arg(finalize_parser)
    finalize_parser.add_argument("--version", required=True)
    finalize_parser.add_argument("--date")

    play_parser = subparsers.add_parser("write-play-notes", help="Write GPP release note files")
    add_app_arg(play_parser)
    play_parser.add_argument("--version", required=True)
    play_parser.add_argument("--track")
    play_parser.add_argument("--output-dir")

    tag_parser = subparsers.add_parser("from-tag", help="Resolve app and version from a release tag")
    tag_parser.add_argument("--tag", required=True)

    args: argparse.Namespace = parser.parse_args()
    if args.command == "extract":
        notes: dict[str, str] = extract_release_notes(args.app, args.version)
        sys.stdout.write(notes[args.locale])
        return 0
    if args.command == "bump-version":
        bump_application_version(args.app, args.version)
        return 0
    if args.command == "finalize":
        finalized: bool = finalize_version(args.app, args.version, args.date)
        if finalized:
            print(f"Finalized [{args.version}] in {app_config(args.app)['changelog']}")
        return 0
    if args.command == "write-play-notes":
        output_dir: Path | None = Path(args.output_dir) if args.output_dir else None
        write_play_release_notes(args.app, args.version, args.track, output_dir)
        return 0
    if args.command == "from-tag":
        app, version = parse_app_from_tag(args.tag)
        print(f"{app} {version}")
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
