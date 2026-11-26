# FR-9: Report the Undo Status

## Statement

When a client requests the undo status of a user, Coffee Kitty MUST report whether the user can undo their most recent
coffee.

The undo status describes the time of the request. If the user can undo their most recent coffee, the undo status MUST
also state the instant at which the grace period of that coffee ends.

Coffee Kitty MUST refuse a request for the undo status of an unknown user with 404 Not Found. The request needs no
acting user, and Coffee Kitty MUST refuse a request with an unknown login name in the header `X-Acting-User` with 401
Unauthorized.

## Rationale

FR-3 lets a user undo their most recent coffee only within its grace period. A client that does not know the end of the
grace period has to offer the undo at any time, and the user learns only from the 409 Conflict that the undo came too
late. With the undo status, a client shows the undo only while Coffee Kitty accepts it.

## Success Criteria

- For a coffee added at 10:00:00, the undo status at 10:02:00 states that the user can undo the coffee until 10:05:00.
- At any time, the undo status states that the user can undo a coffee exactly when an undo at that time lowers the
  coffee count (FR-3).
- A request for the undo status changes neither the coffee count nor the result of a later undo.
- A request for the undo status of an unknown user returns 404 Not Found.
- A request with an unknown login name in `X-Acting-User` returns 401 Unauthorized.

## Verification Method

Test: unit tests with a fixed clock, and system tests against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "The undo button only appears while an undo is possible, and not when the count is 0."
- Depends on FR-3: The success criteria of FR-3 specify when an undo lowers the coffee count and when it returns 409
  Conflict.
- Depends on FR-8: A correction changes which coffee an undo applies to, and the undo status has to follow the undo.
- The second success criterion is a rule, not a single case. The tests of this requirement derive the cases from the
  success criteria of FR-3.
- The coffee app of SE@UHD reports `undoable` and `undoableUntil` in the response of `GET /summary`, and its landing
  page shows the undo button only while `undoable` is true.
- TBD (open): Does a client also need the undo status in the responses to adding and to undoing a coffee, as the coffee
  app of SE@UHD reports it in every summary? Contact person: group lead.
