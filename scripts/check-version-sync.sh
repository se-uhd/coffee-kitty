#!/usr/bin/env bash
# Fail when the latest `## [x.y.z]` header in CHANGELOG.md names a version other than the `version` property in the root
# gradle.properties. The script runs locally and in CI. Each failure prints its message as a GitHub Actions error
# annotation.
set -euo pipefail

cd "$(dirname "$0")/.."

fail() {
  echo "::error::$*" >&2
  exit 1
}

# The `version` property of the root gradle.properties, which Gradle sets as project.version.
gradle=$(sed -n 's/^version *= *\([0-9][^[:space:]]*\)/\1/p' gradle.properties)
[ -n "$gradle" ] || fail "could not read the version from gradle.properties"

# The version in the first `## [x.y.z]` header of CHANGELOG.md, including a pre-release or build suffix. The pattern
# requires a digit after `[`, which skips the `## [Unreleased]` header. The first match is the latest release.
changelog=$(sed -n 's/^## \[\([0-9][^]]*\)\].*/\1/p' CHANGELOG.md | head -n1)
[ -n "$changelog" ] || fail "could not read a released version (## [x.y.z]) from CHANGELOG.md"

echo "Project version: gradle=$gradle changelog=$changelog"

[ "$gradle" = "$changelog" ] ||
  fail "latest CHANGELOG.md version ($changelog) differs from the Gradle build version ($gradle)"

echo "Project versions are consistent ($gradle)."
