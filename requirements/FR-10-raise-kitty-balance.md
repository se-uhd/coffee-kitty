# FR-10: Raise the Kitty Balance

## Statement

When Coffee Kitty records a deposit (FR-4), Coffee Kitty MUST raise the kitty balance by the deposit amount.

Coffee Kitty MUST raise the balance of the user and the kitty balance together or not at all. A deposit that Coffee
Kitty refuses (FR-4) MUST NOT change the kitty balance.

## Rationale

The team assistant puts the cash of each deposit into the kitty and needs to know how much money is in the kitty. Coffee
Kitty does not change the kitty balance yet.

## Success Criteria

- The kitty balance is 0.00 €. After the admin jane_doe records a deposit of 10.00 € for maxmustermann and a deposit of
  2.50 € for student2023, the kitty balance is 12.50 €.
- A request to record a deposit that Coffee Kitty refuses (FR-4) does not change the kitty balance.
- If Coffee Kitty cannot store the kitty balance, the request fails, and Coffee Kitty stores neither the deposit nor the
  raised balance of the user.

## Verification Method

Test: unit tests of the raised kitty balance and the refused requests. Further tests complement them: integration tests
of the stored kitty balance, system tests of the refused requests, and a Cucumber scenario of the first success
criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "A deposit raises their balance and the kitty by the same amount."
- Depends on FR-4: FR-4 specifies which deposits Coffee Kitty records and which requests it refuses.
- FR-16 depends on this requirement: FR-16 reports the kitty balance that a deposit raises.
- This requirement had the ID FR-4b until 2026-11-24. No release of Coffee Kitty contains that ID.
