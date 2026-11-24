# FR-4b: Raise the Kitty Balance

## Statement

When an admin records a deposit between 0.01 € and 1,000.00 € for a user, Coffee Kitty MUST raise the kitty balance by
the deposit amount.

## Rationale

The team assistant puts the cash of each deposit into the kitty and needs to know how much money is in the kitty.
Coffee Kitty does not change the kitty balance yet.

## Success Criteria

- The kitty balance is 0.00 €. After the admin jane_doe records a deposit of 10.00 € for maxmustermann and a deposit of
  2.50 € for student2023, the kitty balance is 12.50 €.
- A request to record a deposit that returns 400 Bad Request or 403 Forbidden (FR-4) does not change the kitty balance.

## Verification Method

Test: unit tests of the raised kitty balance and the refused requests, complemented by integration tests of the stored
kitty balance, system tests of the refused requests, and a Cucumber scenario of the first success criterion. All tests
except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "When someone pays me cash, I record it as a deposit for that person."
- Depends on FR-4: The success criteria of FR-4 specify which requests return 400 Bad Request and 403 Forbidden.
- A deposit raises the balance of the user and the kitty by the same amount (GLOSSARY.md).
