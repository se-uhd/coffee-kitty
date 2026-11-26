# Which limits does the build set for comments, Markdown files, and commit messages?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Agents write most of the text of the repository, and the text grew with every change. On 2026-11-25, 1,719 of the
4,564 lines of production Kotlin were KDoc (38%), because a detekt rule required KDoc on every function. The 268 KDoc
blocks had a median of 6 lines, the 90th percentile 12, and the longest 23. On 2026-11-26, `CLAUDE.md` had 639 lines,
`README.md` 366 (its REST API section 124), `CHANGELOG.md` 349 with 55 bullets of more than 2 lines (the longest 8),
FR-6 122 and FR-8 81. The 82 commits on `main` had subjects with a median of 56 characters, a 90th percentile of 70,
and a maximum of 79, 56 lines of their bodies had more than 72 characters, and none followed Conventional Commits.
A rule in prose does not stop an agent from writing more prose. A check that fails the build does.

## Considered Options

* Keep the rules in `CLAUDE.md` as prose.
* Check new and changed text only, with the current sizes as ceilings for existing files.
* Check all text with fixed limits and shorten the existing files now.

## Decision(s)

The build checks all text with fixed limits (the third option), and the existing text was shortened to meet them.

### Comments (detekt, `config/detekt/detekt.yml`)

* KDoc is required only on contracts: interfaces in a package `ports`, their functions, and `*Properties` classes.
* A KDoc has at most 6 lines of description on a type and 3 on another declaration, and each tag has one line. 6 is
  the median length of the KDoc blocks of 2026-11-25, and 3 is half of it.
* Private members have no KDoc. Other comments are only `// ADR-NNN` or `// External: <link> <reason>` (at most 3
  lines). ForbiddenComment bans filler phrases.
* `gradle detekt` checks `src/main` of the modules, and `gradle detektComments` checks every other Kotlin file.

### Markdown (`config/markdown/limits.yml`, `gradle markdownLimits`)

* Every Markdown file needs an entry, and lines have at most 120 characters outside code blocks and tables.
* `CLAUDE.md` with the unscoped rules has at most 200 lines, the target of the Claude Code documentation. A rule in
  `.claude/rules/` has at most 100 lines, and a `SKILL.md` at most 500, the limit of Anthropic's skill guide.
* `README.md` and `GLOSSARY.md` have at most 200 lines, the README at most 60 per section, a requirement at most 80, and
  an ADR at most 100, which Michael Nygard's "one or two pages" allows. ADR-010 has no line limit, because it records
  every measurement of the build. ADR 016 checks the structure of an ADR.
* `CHANGELOG.md` follows Keep a Changelog 1.1.0, with the sections in its order and bullets of at most 2 lines.

### Commit messages (`scripts/check-commit-message.sh`)

* The subject follows Conventional Commits 1.0.0 with the types feat, fix, docs, refactor, test, build, ci, chore,
  perf, and revert, and has at most 72 characters. The 50 characters of Tim Pope's convention leave too little room
  after the type and the scope, and 72 covers 78 of the 82 subjects of 2026-11-26.
* The body has 1 to 5 bullets of at most 2 lines of 72 characters, followed by the trailers. The bullets state the
  reason for the change, and an ADR records a longer argument.
* The hook in `.githooks/` checks a new commit, and CI checks the commits of a pull request and of a push to `main`.
  The history of `main` was rewritten on 2026-11-26 so that every commit follows the format.

## Consequences

* An agent gets the limit and its source in the message of a failed check, and a Claude Code hook in
  `.claude/settings.json` runs the commit and Markdown checks right after the edit.
* Text that describes the code is deleted rather than moved, so the names and the tests carry more of the meaning.
* The 32 forks of the repository no longer share the history of `main` and need a rebase or a new clone.
* A new Markdown file needs an entry in `config/markdown/limits.yml`, and a new limit needs a new ADR.
