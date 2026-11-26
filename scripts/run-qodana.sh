#!/usr/bin/env bash
# Runs the Qodana scan of .github/workflows/qodana.yml on this machine, which is optional
# (doc/adr/023-static-analysis-in-the-build.md). CI runs the scan only in se-uhd/coffee-kitty, and `gradle build` does
# not run it. The script uses the linter image, the profile, the exclusions, and the fail threshold of qodana.yaml, as
# the workflow does, and it fails when the scan finds a problem. It scans a copy of the files that Git tracks or would
# add, so that it reads no build output and writes nothing into the working tree. It needs Docker and pulls an image of
# about 4 GB.
#
# Usage: scripts/run-qodana.sh
#   QODANA_CACHE  the Docker volume that keeps the caches of Qodana and Gradle between two runs
#                 (default: coffee-kitty-qodana-cache)
# The SARIF report of the last run is in build/qodana/qodana.sarif.json.
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null || ! docker info >/dev/null 2>&1; then
    echo 'Docker is not running. The Qodana scan runs in the linter image of qodana.yaml and needs Docker.' >&2
    exit 2
fi
image=$(sed -n 's/^linter: *//p' qodana.yaml)
if [ -z "$image" ]; then
    echo 'qodana.yaml names no linter image.' >&2
    exit 2
fi

work=$(mktemp -d "${TMPDIR:-/tmp}/qodana.XXXXXX")
trap 'rm -rf "$work" 2>/dev/null || echo "Remove $work by hand." >&2' EXIT
mkdir -p "$work/project" "$work/results" build/qodana
# The files that a commit of the working tree would hold: the tracked ones, except the deleted ones, and the untracked
# ones that Git does not ignore.
git ls-files -z --cached --others --exclude-standard |
    while IFS= read -r -d '' file; do [ -e "$file" ] && printf '%s\0' "$file"; done |
    tar --null -T - -cf - | tar -xf - -C "$work/project"

echo "Scanning with $image and the settings of qodana.yaml."
docker pull --quiet "$image" >/dev/null
status=0
docker run --rm \
    -v "$work/project:/data/project" \
    -v "$work/results:/data/results" \
    -v "${QODANA_CACHE:-coffee-kitty-qodana-cache}:/data/cache" \
    "$image" || status=$?

sarif="$work/results/qodana.sarif.json"
if [ ! -f "$sarif" ]; then
    echo "Qodana wrote no report (exit status $status)." >&2
    exit 2
fi
cp "$sarif" build/qodana/qodana.sarif.json
python3 -I - "$sarif" <<'EOF'
import json
import sys

problems = [
    result
    for run in json.load(open(sys.argv[1]))["runs"]
    for result in run["results"]
    if result.get("baselineState") != "absent" and not result.get("suppressions")
]
for result in problems:
    location = result["locations"][0]["physicalLocation"]
    path = location["artifactLocation"]["uri"].split("$PROJECT_DIR$/")[-1].removeprefix("../")
    line = location.get("region", {}).get("startLine", "?")
    print(f"{path}:{line}: {result['message']['text']} [{result['ruleId']}]")
print(f"Qodana found {len(problems)} problems.")
EOF
if [ "$status" -ne 0 ]; then
    echo "The scan fails the fail threshold of qodana.yaml (exit status $status), as the code-quality job would." >&2
    exit 1
fi
