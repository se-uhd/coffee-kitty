#!/usr/bin/env bash
# Runs scripts/check-commit-message.sh on valid and invalid messages and fails when a result differs from the expected.
set -euo pipefail
cd "$(dirname "$0")"
failures=0

expect() {
    local expected=$1 name=$2 message=$3 file status=0
    file=$(mktemp)
    printf '%b' "$message" >"$file"
    ./check-commit-message.sh "$file" 2>/dev/null || status=$?
    rm -f "$file"
    if [[ $expected == pass && $status -ne 0 ]] || [[ $expected == fail && $status -eq 0 ]]; then
        echo "FAILED: $name (expected $expected, exit code $status)"
        failures=$((failures + 1))
    fi
}

valid='fix(data): name the SQL state of a refused value\n\n- PostgreSQL refuses NUL with SQL state 22021, a data\n  exception.\n- The message now names the SQL state.\n'
expect pass "bullets" "$valid"
expect pass "bullets and trailer" "${valid}\nCo-Authored-By: Claude <noreply@anthropic.com>\n"
expect pass "type without scope" 'docs: shorten the README\n\n- Move the API section to the OpenAPI definition.\n'
expect pass "breaking change" 'feat(api)!: require an acting user\n\n- Every endpoint needs an acting user.\n'
expect pass "comment lines" "${valid}# Please enter the commit message.\n"
expect pass "carriage returns" 'docs: fix a typo\r\n\r\n- Correct the name of the task.\r\n'
expect pass "five bullets" 'chore: tidy\n\n- a\n- b\n- c\n- d\n- e\n'
expect pass "72 characters" "docs: $(printf 'x%.0s' {1..66})\n\n- a\n"
expect pass "umlauts count as one character" "docs: $(printf 'ä%.0s' {1..66})\n\n- a\n"
expect fail "no type" 'Name the SQL state of a refused value\n\n- a\n'
expect fail "unknown type" 'feature: add a chart\n\n- a\n'
expect fail "subject of 73 characters" "docs: $(printf 'x%.0s' {1..67})\n\n- a\n"
expect fail "period at the end" 'docs: fix a typo.\n\n- a\n'
expect fail "no body" 'docs: fix a typo\n'
expect fail "no blank line" 'docs: fix a typo\n- a\n'
expect fail "paragraph body" 'docs: fix a typo\n\nThe README named the wrong task.\n'
expect fail "six bullets" 'chore: tidy\n\n- a\n- b\n- c\n- d\n- e\n- f\n'
expect fail "bullet of three lines" 'chore: tidy\n\n- a\n  b\n  c\n'
expect fail "line of 73 characters" "chore: tidy\n\n- $(printf 'x%.0s' {1..71})\n"
expect fail "blank line between bullets" 'chore: tidy\n\n- a\n\n- b\n'
expect fail "continuation without bullet" 'chore: tidy\n\n  a\n'

if ((failures > 0)); then
    echo "$failures test(s) failed."
    exit 1
fi
echo "All tests passed."
