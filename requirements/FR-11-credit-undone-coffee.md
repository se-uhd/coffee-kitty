# FR-11: Credit an Undone Coffee

## Statement

When a user undoes a coffee, Coffee Kitty MUST raise the user's balance by the amount by which that coffee lowered the
balance.

An undo that Coffee Kitty refuses (FR-3) MUST NOT change the balance. An undo MUST NOT change the kitty balance.

## Rationale

An undo takes back a coffee that a user added by mistake (FR-3), so the undo has to return exactly the amount that the
coffee took. The price per cup can change between a coffee and its undo. If the undo raised the balance by the price at
the time of the undo, a user would gain 0.10 € after the price rose from 0.50 € to 0.60 €.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. The user maxmustermann adds a coffee at
  10:00:00 (balance 1.50 €) and undoes it at 10:02:00. The balance is 2.00 €.
- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. The user maxmustermann adds a coffee at
  10:00:00 (balance 1.50 €). The price per cup changes to 0.60 € at 10:01:00. The user undoes the coffee at 10:02:00.
  The balance is 2.00 €, not 2.10 €.
- An undo that returns 409 Conflict (FR-3) does not change the balance.
- An undo does not change the kitty balance.

## Verification Method

Test: unit tests of the raised balance with a fixed clock. Further tests complement them: integration tests of the
stored price of a coffee and a Cucumber scenario of the second success criterion. All tests except the unit tests run
against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "An undo gives the user back exactly what that coffee cost, even if the price has
  changed since."
- Depends on FR-3: FR-3 specifies when an undo lowers the coffee count and when it returns 409 Conflict.
- Depends on FR-5: FR-5 specifies the amount by which a coffee lowers the balance.
- FR-15 depends on this requirement: FR-15 reports the balance that an undo raises.
- Design, not part of this requirement: Coffee Kitty stores the price of each coffee that a user can still undo, in the
  column `coffee_times.price_cents` (migration V12).
- TBD (resolved): By which amount does a correction (FR-8) change the balance? By the price per cup at the time of the
  correction for each coffee that the correction adds or removes (FR-18). Decided by: team assistant.
- This requirement had the ID FR-5b until 2026-11-24. No release of Coffee Kitty contains that ID.
