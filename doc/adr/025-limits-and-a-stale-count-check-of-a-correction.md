# Which limits does a correction have, and how does it refuse a stale coffee count?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Until 2026-11-26, FR-8 required a note that is not blank, set no upper limit for a corrected coffee count apart from
the largest `Int`, and corrected a count whatever the admin saw. The coffee app of SE@UHD accepts a correction without a
note and a blank note as no note, limits a correction to 1,000,000 (its ADR 035), and refuses a correction with an
optional `expectedCount` that differs from the count with 409 Conflict. Without such a check, an admin who sees a count
of 3 and corrects it to 2 removes a coffee that the user added meanwhile. RFC 9110 gives a conflict with an earlier
change as an example of 409. 412 Precondition Failed belongs to the conditional headers, such as `If-Match`.

## Considered Options

* Keep the rules of FR-8.
* Follow the coffee app, with an `If-Match` header and 412 Precondition Failed for a stale count.
* Follow the coffee app, with the field `expectedCount` of the request body and 409 Conflict for a stale count.

## Decision(s)

Coffee Kitty follows the coffee app with the field `expectedCount` (the third option), because the API reads every other
input from the body or the path, and a client learns the count from the body of a response, not from an `ETag`:

* The note is optional, and the domain service stores a blank note, which includes the no-break space, as no note. The
  constraints on `CountCorrection.note` stay and apply to a note that is not null.
* `CountCorrection.countAfter` has the constraint `@Max(1_000_000)` (ADR 009), so a higher count gets the
  `ValidationException` of every other value error, with 400.
* A correction with an `expectedCount` that differs from the count throws the new `StaleDataException`, which
  `GlobalExceptionHandler` maps to 409. The domain service checks the values first and the expected count second, both
  under the row lock of the consumption.

## Consequences

* Migration V14 allows a correction without a note in `count_corrections.note`.
* A correction to more than 1,000,000 returns 400, so a test reaches the largest coffee count through the data port.
* The TLA+ model has the bound `MaxCorrected` and the step `RefuseStaleCorrection`, and the Alloy model allows a
  correction without a note.
* The solution of assignment 5 for FR-8 refused a correction without a note, which `main` now accepts.
