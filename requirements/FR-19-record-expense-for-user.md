# FR-19: Record an Expense for a User

## Statement

When an admin records an expense for a user, Coffee Kitty MUST raise the balance of that user by the expense amount.

The user is the buyer of the expense. The rules of FR-6 for the kind of expense, the bean, the weight, the amount, and
the note apply. Coffee Kitty MUST store the expense with the buyer, the admin who recorded it, and the data that FR-6
lists. The daily limit of FR-6 MUST NOT refuse an expense that an admin records, also for the admin themselves.

Coffee Kitty MUST refuse an expense that a user who is not an admin records for another user with 403 Forbidden, an
expense for an unknown user with 404 Not Found, and an expense above 1,000.00 € with 400 Bad Request. A refused expense
MUST NOT change the balance. An expense MUST NOT change the kitty balance.

## Rationale

A user, also an admin, records at most 50.00 € per day for themselves (FR-6). A user who paid more for the group,
for example for a spare part of the coffee machine, cannot record that expense and would not get the money back.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After the admin jane_doe records an other expense of 300.00 € with the note
  "Spare part" for maxmustermann, the balance of maxmustermann is 300.00 €, and Coffee Kitty has stored the expense
  with maxmustermann as the buyer and jane_doe as the admin who recorded it.
- maxmustermann has recorded an expense of 50.00 € today. An expense of 10.00 € that jane_doe records for maxmustermann
  on the same day raises the balance of maxmustermann by 10.00 €.
- jane_doe has recorded an expense of 50.00 € for maxmustermann today. A request by maxmustermann to record an expense
  of 0.01 € for themselves on the same day returns 409 Conflict (FR-6).
- jane_doe has recorded own expenses of 50.00 € today (FR-6). An expense of 10.00 € that jane_doe records for herself
  through this requirement raises the balance of jane_doe by 10.00 €.
- An expense of 1,000.00 € that jane_doe records for maxmustermann raises the balance by 1,000.00 €. A request by
  jane_doe to record an expense of 1,000.01 € or of 0.00 € for maxmustermann returns 400 Bad Request, and the balance
  stays the same.
- A request by maxmustermann, who is not an admin, to record an expense for student2023 returns 403 Forbidden, and the
  balance of student2023 stays the same.
- A request by jane_doe to record an expense for an unknown user returns 404 Not Found.
- An expense that jane_doe records for maxmustermann does not change the kitty balance.

## Verification Method

Test: unit tests of the role check, the limit for the amount, the raised balance, and the daily limit that does not
apply. Further tests complement them: integration tests of the stored expense with the buyer and the admin, system
tests of the status codes, and a Cucumber scenario of the first success criterion. All tests except the unit tests run
against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "Everything beyond that needs an admin, and the message says so."
- Input, from the team assistant: "An admin can record an expense for any user and split it: one part is credited to
  the buyer's balance, and the other part is paid straight from the kitty." "The daily limit doesn't apply when an
  admin records an expense for someone." "The limit counts every expense booked to that user on that day, including the
  ones that an admin recorded for them." "If someone records something for someone else, the record shows both."
- Input, from the group lead (2026-11-25): An admin records the expenses that are beyond the daily limit of a user. The
  1,000 € are a hard upper limit for a single amount.
- Depends on FR-6: FR-6 specifies the rules of an expense and the daily limit.
- Depends on FR-14: FR-14 specifies the users and which of them are admins.
- FR-15 depends on this requirement: FR-15 reports the balance that an expense raises.
- Design, not part of this requirement: The table `expenses` has no column for the admin who recorded an expense. The
  implementation of this requirement needs a new migration for it.
- TBD (open): Can an admin pay a part of an expense from the kitty? In the coffee app of SE@UHD, an admin splits an
  expense into a private share, which raises the balance of the buyer, and a kitty share, which lowers the kitty
  balance. Until the team assistant answers this question, the private share of an expense is its full amount, and the
  kitty share is 0.00 € (GLOSSARY.md, private share / kitty share). Contact person: team assistant.
- TBD (open): Can an admin change or delete an expense? Contact person: team assistant.
