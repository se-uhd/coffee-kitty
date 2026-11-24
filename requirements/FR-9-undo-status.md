# FR-9: Report the Undo Status

## Statement

When a client requests the undo status of a user, Coffee Kitty MUST report the time until which the user can undo their
most recent coffee, or that the user cannot undo a coffee.

## Rationale

FR-3 lets a user undo their most recent coffee only within its grace period. A client that does not know the end of
the grace period has to offer the undo at any time, and the user learns only from the 409 Conflict that the undo came
too late. With the undo status, a client shows the undo only while Coffee Kitty accepts it. Dependency: the undo and
its grace period (FR-3).

## Success Criteria

- For a coffee added at 10:00:00, the undo status at 10:02:00 states that the user can undo the coffee until 10:05:00.
- At any time, the undo status states that the user can undo a coffee exactly when an undo at that time lowers the
  coffee count (FR-3).
- A request for the undo status changes neither the coffee count nor the result of a later undo.

## Verification Method

Test: unit tests with a fixed clock, and system tests against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Depends on FR-3: The success criteria of FR-3 specify when an undo lowers the coffee count and when it returns
  409 Conflict.
- The coffee app of SE@UHD reports `undoable` and `undoableUntil` in its summaries, and its landing page shows the undo
  button only while `undoable` is true.
- TBD (open): Does a client also need the undo status in the responses to adding and to undoing a coffee, as the
  coffee app of SE@UHD reports it in every summary? Contact person: group lead.
