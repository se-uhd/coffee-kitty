# How does the build check the structure of an ADR?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Until 2026-11-26, `gradle markdownLimits` checked only the length of an ADR and of its lines (ADR 013). Nothing checked
that an ADR follows `doc/adr/template.md`, which has the headings of the ADR template on the course website
(`assets/docs/adr-template.md`). On 2026-11-26, the 15 ADRs had the sections and the metadata of the template in its
order. ADRs 002 and 006 added a sentence after the status, which the template does not provide. Context had 2 to 11
lines with text (the most in ADR 001), Considered Options 2 to 4 options, Decision(s) 4 to 24 lines (ADR 013) and 72 in
ADR 010, and Consequences 1 to 15 lines (ADR 009).

## Considered Options

* Review the structure of a new ADR by hand.
* Check the structure against headings that the check itself defines.
* Check the structure against the headings and the status line of `doc/adr/template.md`, with a limit for each section.

## Decision(s)

The build checks the structure against the template (the third option), so the template is the only definition of the
structure. `gradle markdownLimits` checks each `doc/adr/NNN-name.md`:

* the name, and the title as the only heading of level 1;
* the sections and the metadata headings of the template, in its order;
* a status from the status line of the template, where Superseded takes "by [ADR-NNN](NNN-name.md)" with an existing
  ADR, an ISO date, and at least one person under Involved;
* 2 to 5 considered options, since a single option records no choice;
* at most 12 lines with text in Context, 30 in Decision(s), and 15 in Consequences;
* numbers from 001 without a gap.

The limits are the longest sections of 2026-11-26, rounded up, and `config/markdown/limits.yml` holds them. ADR 010 has
the structure check without the section limits, because it records every measurement of the build.

## Consequences

* An ADR that follows the template of the course website passes, because both templates have the same headings.
* A change of the headings or of the status line of the template changes the check.
* The template has no status for an ADR that a later ADR changes. The later ADR is named in the Consequences of the
  earlier one, as in ADRs 002 and 006.
