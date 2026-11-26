#!/usr/bin/env bash
# PostToolUse hook: after a Bash command that runs `git commit`, checks the message of the new commit. Exit code 2
# shows the violations to Claude Code (doc/adr/013-limits-for-text.md).
input=$(cat)
[[ $input == *'git commit'* ]] || exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
git rev-parse -q --verify HEAD~1 >/dev/null || exit 0
if ! output=$(scripts/check-commit-message.sh --range HEAD~1..HEAD 2>&1); then
    echo "$output" >&2
    echo "Rewrite the message with git commit --amend." >&2
    exit 2
fi
