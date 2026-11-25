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

Coffee Kitty SHOULD refuse an expense above 100.00 € from a user who is not an admin and an expense above 1,000.00 €
from an admin with 400 Bad Request. Coffee Kitty SHOULD also refuse a bean purchase with a weight below 100 g or above
1,000 g with 400 Bad Request. These limits are plausibility checks: They protect against a typing error and do not state
how much the group spends or buys.

## Rationale

Users buy beans and other supplies for the group with their own money. Today, they hand in receipts, and the team
assistant pays back the money from the kitty or forgets the receipts.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After maxmustermann records a bean purchase of 12.99 € for 500 g of the bean
  "Ethiopia Sidamo", the balance is 12.99 €, and Coffee Kitty has stored the expense with the bean and the weight. After
  maxmustermann records an other expense of 4.50 € with the note "Filters", the balance is 17.49 €.
- An expense of 0.01 € raises a balance of 0.00 € to 0.01 €, and an expense of 100.00 € by maxmustermann, who is not an
  admin, raises a balance of 0.00 € to 100.00 €.
- A request to record an expense of 0.00 € returns 400 Bad Request, and a request by maxmustermann to record an expense
  of 100.01 € returns 400 Bad Request. The balance stays the same.
- The admin jane_doe records an own expense of 1,000.00 €, and the balance of jane_doe rises by 1,000.00 €. A request by
  jane_doe to record an expense of 1,000.01 € returns 400 Bad Request.
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

Test: unit tests of the limits for the amount, the weight, the name of the bean, and the note, of the rules for bean
purchases and other expenses, and of the raised balance. Further tests complement them: integration tests of the stored
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
- Input, from the group lead (2026-11-25): Nobody pays 1,000 € into the kitty, and a usual amount is at most about 100
  €. A user can therefore enter at most 100 €. An expense can be above 100 €, for example for a spare part of the coffee
  machine or for a new machine, so an admin can enter up to 1,000 €. Both limits are plausibility checks, not rules on
  what the group pays or spends. This input lowers the limit of the team assistant for a user who is not an admin.
- Input, from the group lead (2026-11-25): The limits for the weight are plausibility checks as well. A bag of beans
  weighs at least 100 g, and a weight above 1,000 g is more likely a typing error than one bag, such as a weight in
  kilograms that someone enters as grams. A user who buys more records several bean purchases.
- This requirement covers only an expense that a user records for themselves. Coffee Kitty cannot check with whose money
  the user paid, so the private share of such an expense is its full amount, and the kitty share is 0.00 € (GLOSSARY.md,
  private share / kitty share).
- Depends on FR-14: FR-14 specifies the users.
- FR-12 and FR-15 depend on this requirement: A bean purchase creates the beans that users rate (FR-12), and FR-15
  reports the balance that an expense raises.
- TBD (open): How does a user who is not an admin record an expense above 100 €, for example for a spare part of the
  coffee machine? In the coffee app of SE@UHD, an admin records an expense for any user. Until the group lead answers
  this question, this requirement has no function for an admin to record an expense for another user. Contact person:
  group lead.
- TBD (open): Can users change or delete their own expenses? Contact person: team assistant.
- TBD (open): Is there a daily limit for the expenses that users record themselves? Contact person: team assistant.
- TBD (open): How does Coffee Kitty record the cash that the team assistant pays a user from the kitty for a positive
  balance? A deposit cannot be negative (FR-4), and a kitty adjustment belongs to no user (GLOSSARY.md). Contact person:
  team assistant.
- TBD (open): How does Coffee Kitty make sure that a user records an expense only for themselves? Coffee Kitty has no
  authentication yet. Contact person: group lead.
