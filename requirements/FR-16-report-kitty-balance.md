# FR-16: Report the Kitty Balance

## Statement

When a user requests the kitty balance, Coffee Kitty MUST report the kitty balance in euro cents.

The kitty balance of a new installation is 0.00 €. The response MUST NOT state which user paid which amount. Coffee
Kitty MUST refuse a request without a known acting user with 401 Unauthorized.

## Rationale

The team assistant never knows exactly how much money should be in the kitty. With the kitty balance, the team assistant
compares the cash in the kitty with the amount that Coffee Kitty reports, and the users see whether the group can buy
new beans.

## Success Criteria

- On a new installation, the kitty balance is 0.00 €.
- After the admin jane_doe records a deposit of 10.00 € for maxmustermann and a deposit of 2.50 € for student2023
  (FR-10), the kitty balance is 12.50 €.
- A request for the kitty balance without a known acting user returns 401 Unauthorized.

## Verification Method

Test: system tests of the reported kitty balance and the status code, and the Cucumber scenario of FR-10, which reads
the kitty balance. All tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "The app keeps track of the kitty's balance." "Users see the kitty's balance, but not
  who paid how much."
- Depends on FR-10: FR-10 specifies how a deposit raises the kitty balance.
- The kitty balance of a new installation is 0.00 €, because no requirement specifies a kitty adjustment yet
  (GLOSSARY.md), with which an admin records the cash that is in the kitty at the start.
