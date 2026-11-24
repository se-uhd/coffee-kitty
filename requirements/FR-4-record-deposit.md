# FR-4: Record a Deposit

## Statement

When an admin records a deposit between 0.01 € and 1,000.00 € for a user, Coffee Kitty MUST raise the user's balance by
the deposit amount.

## Rationale

Users pay what they owe in cash to the team assistant, who is an admin and puts the cash into the kitty.
Coffee Kitty does not record this cash yet.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After the admin jane_doe records a deposit of 10.00 € for maxmustermann, the
  balance is 10.00 €. After jane_doe records a second deposit of 2.50 € for maxmustermann, the balance is 12.50 €.
- A deposit of 0.01 € raises a balance of 0.00 € to 0.01 €, and a deposit of 1,000.00 € raises a balance of 0.00 € to
  1,000.00 €.
- A request by jane_doe to record a deposit of 0.00 € or of 1,000.01 € returns 400 Bad Request, and the balance stays
  the same.
- A request by maxmustermann, who is not an admin, to record a deposit returns 403 Forbidden, and the balance stays the
  same.
- A deposit with a note of 500 characters raises the balance. A request to record a deposit with a note of 501
  characters returns 400 Bad Request, and the balance stays the same.

## Verification Method

Test: unit tests of the limits for the amount and the note, the role check, and the raised balance. Further tests
complement them: integration tests of the stored balance, system tests of the status codes, and a Cucumber scenario of
the first success criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "When someone pays me cash, I record it as a deposit for that person."
- Depends on the balance of a user (GLOSSARY.md).
- FR-10 depends on this requirement: A deposit also raises the kitty balance by the deposit amount.
- A note is optional and has at most 500 characters (GLOSSARY.md).
