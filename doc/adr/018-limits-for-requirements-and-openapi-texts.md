# Which limits apply to a requirement and to the texts of the OpenAPI annotations?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

ADR 013 limited a requirement to 80 lines. To meet the limit, FR-6 was shortened from 122 to 79 lines and FR-8 from 81
to 78. On 2026-11-26, a review compared both statement by statement with their earlier versions and found 12 changes of
meaning in FR-6 and 4 in FR-8, such as success criteria that no longer named who records an expense. FR-6 needs 101
lines to keep its meaning. On the same day, the 42 texts of the springdoc annotations in `api/` had no limit. The 9
summaries had a median of 30 characters, a 90th percentile of 68, and a maximum of 69. The 31 descriptions of
parameters, schemas, request bodies, and tags had a median of 44, a 90th percentile of 83, and a maximum of 162.

## Considered Options

* Keep the limit of 80 lines, shorten FR-6 further, and leave the OpenAPI texts without a limit.
* Remove the limit for requirements.
* Take the limit for requirements from the longest requirement whose meaning a review confirmed, and limit the OpenAPI
  texts after published lint rules.

## Decision(s)

The build uses the third option:

* A requirement has at most 110 lines, which is FR-6 with 101 lines, rounded up. A requirement is shortened only after a
  comparison of each of its statements with the earlier version.
* The detekt rule `OpenApiTextLength` (`config/detekt/detekt.yml`) limits a `summary` or a `title` to 60 characters and
  a summary to one sentence. The rules for summaries in the documentation of
  [Redocly](https://redocly.com/docs/cli/rules/configurable-rules) and
  [Zuplo](https://zuplo.com/docs/articles/api-lint-custom-rules) use 60, so that a summary fits the sidebar of a
  developer portal.
* A `description` has at most 120 characters, the line length of the code. The description of `@Operation` or `@Info`
  has at most 360, three lines, for the "verbose explanation" that the
  [OpenAPI Specification 3.1.0](https://spec.openapis.org/oas/v3.1.0.html) allows there.
* ADR-010 records the measurements.

## Consequences

* FR-6 keeps its meaning, and FR-8 has its text from before ADR 013 again.
* Three summaries and the description of the tag of the consumption endpoints were shortened to meet the limits.
* A new kind of OpenAPI annotation with a text needs an entry in `OpenApiTextLength`.
