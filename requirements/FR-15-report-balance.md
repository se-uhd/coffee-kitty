# FR-15: Report a Balance

## Statement

When a user requests the balance of a user, Coffee Kitty MUST report that balance in euro cents.

The balance of a new user is 0.00 €. Coffee Kitty MUST refuse a request for the balance of an unknown user with 404 Not
Found and a request without a known acting user with 401 Unauthorized.

## Rationale

With the tally sheet, nobody knows how much they owe until the team assistant has counted the strokes. Users want to see
what they owe at any time, and the team assistant wants to see the money without counting.

## Success Criteria

- The admin jane_doe creates the user new_user. The balance of new_user is 0.00 €.
- After jane_doe records a deposit of 10.00 € for maxmustermann (FR-4), the balance of maxmustermann is 10.00 €.
- A request for the balance of an unknown user returns 404 Not Found.
- A request for a balance without a known acting user returns 401 Unauthorized.

## Verification Method

Test: system tests of the reported balance and the status codes, and the Cucumber scenarios of FR-4, which read the
balance. All tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- User requirement (parent): "I want to add a coffee with one tap and see how much I owe the kitty."
- Input, from the team assistant: "Users see their own balance and the current price per cup." "A new user has a balance
  of 0.00 €." "A negative balance means that the user owes the kitty money. A positive balance means that the kitty owes
  the user money."
- Depends on FR-4, FR-5, FR-6, FR-11, and FR-18: These requirements specify what changes a balance: a deposit (FR-4), a
  coffee (FR-5), an expense (FR-6), an undo (FR-11), and a correction (FR-18).
- TBD (open): Who is allowed to read the balance of another user? In the coffee app of SE@UHD, a user sees only their
  own balance, and an admin sees every balance. Coffee Kitty has no authentication yet. Until the group lead answers
  this question, this requirement lets every known acting user read every balance. Contact person: group lead.
