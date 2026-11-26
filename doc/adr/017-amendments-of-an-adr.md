# How does an ADR record that a later ADR changes a part of it?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

`CLAUDE.md` allows only the status of an accepted ADR to change, and a changed decision needs a new ADR. The statuses of
`doc/adr/template.md` are Proposed, Rejected, Accepted, Deprecated, and Superseded, and Superseded replaces a whole ADR.
ADRs 002 and 006 named a later ADR that changes a part of them in a sentence after "Accepted". On 2026-11-26, ADR 016
moved these sentences into the Consequences of ADRs 002 and 006, and ADR 013 changed its limit for requirements in its
own text. Both edits changed accepted ADRs beyond their status.

## Considered Options

* Name the later ADR in the Consequences of the earlier one, as ADR 016 does.
* Set the status of the earlier ADR to Superseded, although only a part of it changes.
* Add the later ADR to the status of the earlier one: "Accepted, amended by [ADR-NNN](NNN-name.md)".

## Decision(s)

The status names each later ADR that changes a part of the ADR (the third option), because the status is the only part
of an accepted ADR that may change:

* An accepted ADR that a later ADR changes in part has the status "Accepted, amended by [ADR-NNN](NNN-name.md)", with
  one link for each later ADR, separated by ", ". The body stays as it was accepted.
* `gradle markdownLimits` accepts this suffix after each status of the template except Superseded, and each link has to
  name an existing ADR.
* This ADR replaces the last consequence of ADR 016. ADRs 002, 006, and 013 get back the text that they had when they
  were accepted, and ADRs 002, 006, 013, and 016 get the new status.

## Consequences

* The status of an ADR shows which later ADRs a reader has to read with it.
* The ADR template of the course website does not list the suffix. An ADR without a later change still has a status of
  the template.
* The sentences after "Accepted" in ADRs 002 and 006 said what ADR 009 and ADR 008 change, which these ADRs state.
