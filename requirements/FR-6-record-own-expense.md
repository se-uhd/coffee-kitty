# FR-6: Record an Own Expense

## Statement

When a user records an expense, Coffee Kitty MUST raise the user's balance by the expense amount.

An expense is either a bean purchase or an other expense. A bean purchase has a bean and a weight, and an other expense
has neither. An expense amount is positive, so the smallest expense is 0.01 €. A request MAY contain a note of at most
500 characters, and a blank note counts as no note.

Coffee Kitty MUST store the expense with the buyer, the amount, the kind of expense, the bean and the weight of a bean
purchase, the note, and the time. A bean purchase names its bean. A name that no bean has creates the bean, and a name
that differs only in case from the name of a bean refers to that bean. A name has at most 200 characters, and Coffee
Kitty removes the spaces at its start and end.

Coffee Kitty MUST refuse an expense that breaks one of these rules with 400 Bad Request. A refused expense MUST NOT
change the balance. An expense MUST NOT change the kitty balance.

A user who is not an admin has a daily limit: at most 10 expenses and at most 50.00 € in total on one day. Coffee Kitty
MUST refuse an expense that exceeds the daily limit with 409 Conflict. A day starts at midnight in the time zone
Europe/Berlin. The group MAY configure another amount for the daily limit, and Coffee Kitty MUST NOT start with a
configured amount above 100.00 €. The daily limit does not apply to an admin.

Coffee Kitty SHOULD refuse an expense above 1,000.00 € with 400 Bad Request. Coffee Kitty SHOULD also refuse a bean
purchase with a weight below 100 g or above 1,000 g with 400 Bad Request. These limits are plausibility checks: They
protect against a typing error and do not state how much the group spends or buys.

## Rationale

Users buy beans and other supplies for the group with their own money. Today, they hand in receipts, and the team
assistant pays back the money from the kitty or forgets the receipts.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After maxmustermann records a bean purchase of 12.99 € for 500 g of the bean
  "Ethiopia Sidamo", the balance is 12.99 €, and Coffee Kitty has stored the expense with the bean and the weight. After
  maxmustermann records an other expense of 4.50 € with the note "Filters", the balance is 17.49 €.
- An expense of 0.01 € raises a balance of 0.00 € to 0.01 €. A request to record an expense of 0.00 € returns 400 Bad
  Request, and the balance stays the same.
- maxmustermann, who is not an admin, has recorded no expense today. An expense of 50.00 € by maxmustermann raises the
  balance by 50.00 €. A request by maxmustermann to record a further expense of 0.01 € on the same day returns 409
  Conflict, and the balance stays the same.
- maxmustermann has recorded expenses of 30.00 € and 19.99 € today. A request by maxmustermann to record an expense of
  0.02 € returns 409 Conflict, and an expense of 0.01 € raises the balance.
- maxmustermann has recorded 10 expenses of 1.00 € today. A request by maxmustermann to record an eleventh expense of
  1.00 € returns 409 Conflict, and the balance stays the same.
- maxmustermann recorded an expense of 50.00 € at 23:59 in Europe/Berlin. An expense of 50.00 € by maxmustermann at
  00:00 of the next day raises the balance.
- With a configured amount of 100.00 € for the daily limit, an expense of 100.00 € by maxmustermann raises the balance,
  and a request by maxmustermann to record an expense of 100.01 € returns 409 Conflict. Coffee Kitty does not start with
  a configured amount of 100.01 €.
- The admin jane_doe records an own expense of 1,000.00 €, and the balance of jane_doe rises by 1,000.00 €, because the
  daily limit does not apply to an admin. A request by jane_doe to record an expense of 1,000.01 € returns 400 Bad
  Request.
- Bean purchases of 100 g and of 1,000 g raise the balance. A request to record a bean purchase of 99 g or of 1,001 g
  returns 400 Bad Request, and the balance stays the same.
- A request to record a bean purchase without a bean or without a weight returns 400 Bad Request, and the balance stays
  the same.
- A request to record an other expense with a bean or with a weight returns 400 Bad Request, and the balance stays the
  same.
- An expense with a note of 500 characters raises the balance. A request to record an expense with a note of 501
  characters returns 400 Bad Request, and the balance stays the same.
- No bean has the name "Ethiopia Sidamo". After a bean purchase of the bean "Ethiopia Sidamo", Coffee Kitty has one bean
  with this name. After a second bean purchase with the name "ethiopia sidamo", Coffee Kitty still has one bean with
  this name, and both bean purchases refer to it.
- A bean purchase with a name of 200 characters raises the balance. A request to record a bean purchase with a name of
  201 characters returns 400 Bad Request, and the balance stays the same.
- An expense does not change the kitty balance.

## Verification Method

Test: unit tests of the limits for the amount, the weight, the name of the bean, and the note, of the daily limit with a
fixed clock, of the rules for bean purchases and other expenses, and of the raised balance. Further tests complement them: integration tests of the stored
expense, the stored bean, and the stored balance, system tests of the status codes, and a Cucumber scenario of the first
success criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "When someone buys something for the group with their own money, such as a bag of
  beans, they record it as an expense."
- Input, from the team assistant: "A bean purchase has the name of the beans and the weight in grams, between 100 g and
  1,000 g. Other expenses have neither." "Every expense has an amount between 0.01 € and 1,000 € and an optional note of
  up to 500 characters." "A bean gets into the catalog when someone records a bean purchase with a name that's new."
  "Bean names are unique, regardless of upper and lower case, and have at most 200 characters. Extra spaces are
  removed."
- Input, from the team assistant: "A user can record at most 10 expenses per day, and at most 50 € in total per day.
  Reaching exactly 50 € is fine. Everything beyond that needs an admin, and the message says so. The day starts at
  midnight, German time."
- Input, from the group lead (2026-11-25): The daily limit is the only limit for the expenses that users record
  themselves. Its amount is a setting, which the group can raise to at most 100 €. An expense can be higher, for example
  for a spare part of the coffee machine or for a new machine, so the daily limit does not apply to an admin. The limit
  of 1,000 € for a single amount stays as a plausibility check, not as a rule on what the group pays or spends.
- Input, from the group lead (2026-11-25): The limits for the weight are plausibility checks as well. A bag of beans
  weighs at least 100 g, and a weight above 1,000 g is more likely a typing error than one bag, such as a weight in
  kilograms that someone enters as grams. A user who buys more records several bean purchases.
- This requirement covers only an expense that a user records for themselves. Coffee Kitty cannot check with whose money
  the user paid, so the private share of such an expense is its full amount, and the kitty share is 0.00 € (GLOSSARY.md,
  private share / kitty share).
- Depends on FR-14: FR-14 specifies the users and which of them are admins.
- Design, not part of this requirement: Coffee Kitty stores every time in UTC, so the check of the daily limit has to
  convert the time of an expense to Europe/Berlin.
- FR-12 and FR-15 depend on this requirement: A bean purchase creates the beans that users rate (FR-12), and FR-15
  reports the balance that an expense raises.
- TBD (open): How does a user who is not an admin record an expense beyond the daily limit, for example for a spare part
  of the coffee machine? In the coffee app of SE@UHD, an admin records an expense for any user, and the daily limit
  does not apply to such an expense. Until the group lead answers this question, this requirement has no function for
  an admin to record an expense for another user, and the response to a refused expense does not name one. Contact
  person: group lead.
- TBD (open): Can users change or delete their own expenses? Contact person: team assistant.
- TBD (resolved): Is there a daily limit for the expenses that users record themselves? Yes, at most 10 expenses and
  50.00 € on one day, as in the coffee app of SE@UHD. The amount can be configured up to 100.00 €. Decided by: group
  lead (2026-11-25).
- TBD (open): How does Coffee Kitty record the cash that the team assistant pays a user from the kitty for a positive
  balance? A deposit cannot be negative (FR-4), and a kitty adjustment belongs to no user (GLOSSARY.md). Contact person:
  team assistant.
- TBD (open): How does Coffee Kitty make sure that a user records an expense only for themselves? Coffee Kitty has no
  authentication yet. Contact person: group lead.
