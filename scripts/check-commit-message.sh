#!/usr/bin/env bash
# Checks commit messages against the format of doc/adr/013-limits-for-text.md.
#
#   scripts/check-commit-message.sh <file>             checks the message in <file>, as the commit-msg hook does
#   scripts/check-commit-message.sh --range <a>..<b>   checks each commit in the range, except merges and Dependabot
#
# Exits with 1 and prints each violation when a message breaks the format.
set -euo pipefail
export LC_ALL=C.UTF-8

readonly MAX_SUBJECT=72
readonly MAX_LINE=72
readonly MAX_BULLETS=5
readonly MAX_BULLET_LINES=2
readonly SUBJECT='^(feat|fix|docs|refactor|test|build|ci|chore|perf|revert)(\([a-z0-9-]+\))?!?: [^ ]'
readonly TRAILER='^[A-Za-z][A-Za-z0-9-]*: .+'

errors=0

fail() {
    echo "$1: $2" >&2
    errors=$((errors + 1))
}

# Checks the message on stdin. $1 names the message in the output.
check_message() {
    local label=$1 line
    local lines=()
    while IFS= read -r line || [[ -n $line ]]; do
        line=${line%$'\r'}
        [[ $line == \#* ]] && continue
        lines+=("$line")
    done
    local n=${#lines[@]}
    while ((n > 0)) && [[ -z ${lines[n - 1]// /} ]]; do n=$((n - 1)); done
    if ((n == 0)); then
        fail "$label" "the message is empty"
        return
    fi

    local subject=${lines[0]}
    [[ $subject =~ $SUBJECT ]] ||
        fail "$label" "the subject '$subject' must have the form 'type(scope): description' with a type of feat, fix, docs, refactor, test, build, ci, chore, perf, or revert"
    ((${#subject} <= MAX_SUBJECT)) || fail "$label" "the subject has ${#subject} characters, more than $MAX_SUBJECT"
    [[ $subject != *. ]] || fail "$label" "the subject ends with a period"
    if ((n < 3)) || [[ -n ${lines[1]} ]]; then
        fail "$label" "a blank line and at least one bullet must follow the subject"
        return
    fi

    # The trailers, such as Co-Authored-By, form the last paragraph after a blank line.
    local end=$n start=$n
    while ((start > 2)) && [[ -n ${lines[start - 1]} ]]; do start=$((start - 1)); done
    if ((start > 2)); then
        local i trailers=1
        for ((i = start; i < n; i++)); do [[ ${lines[i]} =~ $TRAILER ]] || trailers=0; done
        ((trailers == 1)) && end=$((start - 1))
    fi

    local i bullets=0 bullet_lines=0
    for ((i = 2; i < end; i++)); do
        line=${lines[i]}
        if [[ $line =~ ^-\ [^\ ] ]]; then
            bullets=$((bullets + 1))
            bullet_lines=1
        elif [[ $line =~ ^\ \ [^\ ] ]] && ((bullets > 0)); then
            bullet_lines=$((bullet_lines + 1))
            ((bullet_lines != MAX_BULLET_LINES + 1)) ||
                fail "$label" "bullet $bullets has more than $MAX_BULLET_LINES lines"
        else
            fail "$label" "line $((i + 1)) is neither a bullet ('- ') nor its continuation (two spaces): '$line'"
        fi
        ((${#line} <= MAX_LINE)) || fail "$label" "line $((i + 1)) has ${#line} characters, more than $MAX_LINE"
    done
    ((bullets > 0)) || fail "$label" "the body has no bullet"
    ((bullets <= MAX_BULLETS)) || fail "$label" "the body has $bullets bullets, more than $MAX_BULLETS"
}

if [[ ${1:-} == --range ]]; then
    while read -r sha email; do
        [[ $email == *dependabot* ]] && continue
        check_message "commit ${sha:0:7}" < <(git log -1 --format=%B "$sha")
    done < <(git log --no-merges --format='%H %ae' "$2")
elif [[ $# -eq 1 ]]; then
    check_message "commit message" <"$1"
else
    echo "Usage: $0 <message file> | --range <a>..<b>" >&2
    exit 2
fi

if ((errors > 0)); then
    echo "Write the message as doc/adr/013-limits-for-text.md describes, for example:" >&2
    printf '  %s\n' "fix(data): name the SQL state of a refused value" "" \
        "- PostgreSQL refuses NUL with SQL state 22021." "- The message now names the SQL state." >&2
    exit 1
fi
