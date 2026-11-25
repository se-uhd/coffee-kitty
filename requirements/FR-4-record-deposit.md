# FR-4: Record a Deposit

## Statement

When an admin records a deposit for a user, Coffee Kitty MUST raise the user's balance by the deposit amount.

A deposit amount is positive, so the smallest deposit is 0.01 €. A request MAY contain a note of at most 500 characters,
and a blank note counts as no note. Coffee Kitty MUST store the deposit with the user, the amount, the note, and the
time.

Coffee Kitty MUST refuse a deposit that a user who is not an admin records with 403 Forbidden. Coffee Kitty MUST refuse
a deposit with an amount that is not positive or with a note of more than 500 characters with 400 Bad Request, and a
deposit for an unknown user with 404 Not Found. A refused deposit MUST NOT change the balance.

Coffee Kitty MUST refuse a deposit above 1,000.00 € with 400 Bad Request. This limit protects against a typing error
and does not state how much a user pays.

## Rationale

Users pay what they owe (FR-5) in cash to the team assistant, who is an admin and puts the cash into the kitty. Coffee
Kitty does not record this cash yet.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After the admin jane_doe records a deposit of 10.00 € for maxmustermann, the
  balance is 10.00 €, and Coffee Kitty has stored a deposit of 10.00 € for maxmustermann. After jane_doe records a
  second deposit of 2.50 € for maxmustermann, the balance is 12.50 €.
- A deposit of 0.01 € raises a balance of 0.00 € to 0.01 €, and a deposit of 1,000.00 € raises a balance of 0.00 € to
  1,000.00 €.
- A request by jane_doe to record a deposit of 0.00 € or of 1,000.01 € returns 400 Bad Request, and the balance stays
  the same.
- A request by maxmustermann, who is not an admin, to record a deposit of 10.00 € or of 0.00 € returns 403 Forbidden,
  and the balance stays the same.
- A deposit with a note of 500 characters raises the balance. A request to record a deposit with a note of 501
  characters returns 400 Bad Request, and the balance stays the same.
- A deposit with a blank note raises the balance, and Coffee Kitty stores the deposit without a note.
- A request by jane_doe to record a deposit for an unknown user returns 404 Not Found.

## Verification Method

Test: unit tests of the limits for the amount and the note, the role check, and the raised balance. Further tests
complement them: integration tests of the stored deposit and the stored balance, system tests of the status codes, and a
Cucumber scenario of the first success criterion. All tests except the unit tests run against PostgreSQL in
Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "When someone pays me cash, I record it as a deposit for that person."
- Input, from the team assistant: "A deposit is always positive and at most 1,000 €. A note is optional." "Only admins
  record deposits and adjustments. Users can't record their own deposits." "Notes have at most 500 characters."
- Input, from the group lead (2026-11-25): Nobody pays 1,000 € into the kitty, and a usual deposit is at most about 100
  €. The 1,000 € are a hard upper limit for a single amount, which protects against a typing error of the admin who
  records the deposit.
- Depends on FR-14: FR-14 specifies the users and which of them are admins.
- FR-10 and FR-15 depend on this requirement: A deposit also raises the kitty balance by the deposit amount (FR-10), and
  FR-15 reports the balance that a deposit raises.
- The smallest deposit is 0.01 €, because Coffee Kitty counts money in euro cents (GLOSSARY.md).
