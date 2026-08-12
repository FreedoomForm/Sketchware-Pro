#!/usr/bin/env python3
"""
Bump versionCode + versionName in app/build.gradle for auto-release.

Strategy:
  - Read current versionCode and versionName from defaultConfig.
  - Increment versionCode by 1.
  - Bump the patch component of versionName by 1 (v7.0.0 -> v7.0.1,
    v7.0.1 -> v7.0.2, etc.). If versionName does not match the
    `v<major>.<minor>.<patch>` pattern, fall back to appending -b<code>.
  - Append a build-suffix derived from the short commit SHA so each
    auto-release is uniquely identifiable in the in-app version list
    (e.g. v7.0.1+740f984). This suffix is stripped before the next bump
    so versionName stays monotonic across releases.
  - Write the modified build.gradle back to disk in place.

The script is idempotent in the sense that running it twice produces
two different versionCodes — there is no risk of an accidental
no-op release.

Invoked by .github/workflows/release.yml on every push to main.
"""

import re
import subprocess
import sys
from pathlib import Path

BUILD_GRADLE = Path(__file__).resolve().parents[2] / "app" / "build.gradle"

VERSION_CODE_RE = re.compile(r"^(\s*versionCode\s*=\s*)(\d+)(\s*)$", re.MULTILINE)
VERSION_NAME_RE = re.compile(r"^(\s*versionName\s*=\s*)\"([^\"]*)\"(\s*)$", re.MULTILINE)


def short_sha() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short=7", "HEAD"], text=True
        ).strip()
    except Exception:
        return "unknown"


def strip_build_suffix(name: str) -> str:
    """Drop a trailing +suffix or -bsuffix from versionName."""
    return re.split(r"[+\-]b", name, maxsplit=1)[0]


def bump_patch(name: str) -> str:
    """v7.0.0 -> v7.0.1; v7.0 -> v7.0.1; unknown -> unknown.1"""
    m = re.match(r"^(v?)(\d+)(?:\.(\d+))?(?:\.(\d+))?$", name)
    if not m:
        return name + ".1"
    prefix, major, minor, patch = m.group(1), m.group(2), m.group(3), m.group(4)
    major_i = int(major)
    minor_i = int(minor) if minor else 0
    patch_i = int(patch) if patch else 0
    patch_i += 1
    return f"{prefix}{major_i}.{minor_i}.{patch_i}"


def main() -> int:
    if not BUILD_GRADLE.exists():
        print(f"ERROR: build.gradle not found at {BUILD_GRADLE}", file=sys.stderr)
        return 1

    text = BUILD_GRADLE.read_text(encoding="utf-8")

    m = VERSION_CODE_RE.search(text)
    if not m:
        print("ERROR: versionCode line not found in build.gradle", file=sys.stderr)
        return 2
    old_code = int(m.group(2))
    new_code = old_code + 1

    m = VERSION_NAME_RE.search(text)
    if not m:
        print("ERROR: versionName line not found in build.gradle", file=sys.stderr)
        return 3
    old_name = m.group(2)
    base_name = strip_build_suffix(old_name)
    new_name_base = bump_patch(base_name)
    sha = short_sha()
    new_name = f"{new_name_base}+{sha}"

    new_text = VERSION_CODE_RE.sub(
        lambda mm: f"{mm.group(1)}{new_code}{mm.group(3)}", text, count=1
    )
    new_text = VERSION_NAME_RE.sub(
        lambda mm: f'{mm.group(1)}"{new_name}"{mm.group(3)}', new_text, count=1
    )

    BUILD_GRADLE.write_text(new_text, encoding="utf-8")

    # Emit GitHub Actions outputs for downstream steps.
    if "GITHUB_OUTPUT" in __import__("os").environ:
        out = Path(__import__("os").environ["GITHUB_OUTPUT"])
        with out.open("a", encoding="utf-8") as f:
            f.write(f"version_code={new_code}\n")
            f.write(f"version_name={new_name}\n")
            f.write(f"version_name_base={new_name_base}\n")

    print(f"Bumped versionCode: {old_code} -> {new_code}")
    print(f"Bumped versionName: {old_name} -> {new_name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
