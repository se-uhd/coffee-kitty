#!/usr/bin/env bash
# PostToolUse hook: after an edit of a Markdown file, runs the task markdownLimits. Exit code 2 shows the violations to
# Claude Code. Other failures of Gradle do not block (doc/adr/013-limits-for-text.md).
input=$(cat)
[[ $input =~ \"file_path\":\"[^\"]*\.md\" ]] || exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
output=$(mise exec -- gradle -q --console=plain markdownLimits 2>&1) && exit 0
if [[ $output == *'break the limits of config/markdown/limits.yml'* ]]; then
    grep -E '\.md(:[0-9]+)?: ' <<<"$output" >&2
    exit 2
fi
exit 0
