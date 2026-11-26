# FR-6: Record an Own Expense

## Statement

When a user records an expense, Coffee Kitty MUST raise the user's balance by the expense amount.

An expense is either a bean purchase or an other expense. A bean purchase has a bean and a weight, and an other expense
has neither. An expense amount is positive, so the smallest expense is 0.01 €. A request MAY contain a note of at most
500 characters, and a blank note counts as no note. Coffee Kitty MUST store the expense with the buyer, the amount, the
kind of expense, the bean and the weight of a bean purchase, the note, and the time. A bean purchase names its bean. A
name that no bean has creates the bean, and a name that differs only in case from the name of a bean refers to that
bean. A name has at most 200 characters, and Coffee Kitty removes the spaces at its start and end.

Coffee Kitty MUST refuse an expense that breaks one of these rules or is above 1,000.00 € with 400 Bad Request. Coffee
Kitty SHOULD refuse a bean purchase with a weight below 100 g or above 1,000 g with 400 Bad Request. The limits of
1,000.00 € and of the weight protect against a typing error and do not state how much the group spends or buys. A
refused expense MUST NOT change the balance. An expense MUST NOT change the kitty balance.

Every user has a daily limit for their own expenses, also an admin: at most 5 expenses and at most 50.00 € in total on
one day. The daily limit counts every expense of the user on that day, also an expense that an admin recorded for the
user (FR-19). Coffee
Kitty MUST refuse an expense that exceeds the daily limit with 409 Conflict, and the message of the response MUST state
that an admin can record the expense. A day starts at midnight in the time zone Europe/Berlin. The group MAY configure
another amount for the daily limit, and Coffee Kitty MUST NOT start with a configured amount above 100.00 €. The daily
limit MUST NOT refuse an expense that an admin records for a user, also for the admin themselves (FR-19).

## Rationale

Users buy beans and other supplies for the group with their own money. Today, they hand in receipts, and the team
assistant pays back the money from the kitty or forgets the receipts.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After maxmustermann records a bean purchase of 12.99 € for 500 g of the bean
  "Ethiopia Sidamo", the balance is 12.99 €, and Coffee Kitty has stored the expense with the bean and the weight. After
  maxmustermann records an other expense of 4.50 € with the note "Filters", the balance is 17.49 €.
- An expense of 0.01 € raises a balance of 0.00 € to 0.01 €. Recording an expense of 0.00 € returns 400 Bad Request,
  and the balance stays the same.
- maxmustermann, who is not an admin, has recorded no expense today. An expense of 50.00 € by maxmustermann raises the
  balance by 50.00 €. Recording a further expense of 0.01 € by maxmustermann today returns 409 Conflict with a message
  that names an admin, and the balance stays the same.
- maxmustermann has recorded expenses of 30.00 € and 19.99 € today. Recording an expense of 0.02 € by maxmustermann
  returns 409 Conflict, and an expense of 0.01 € raises the balance.
- maxmustermann has recorded 5 expenses of 1.00 € today. Recording a sixth expense of 1.00 € by maxmustermann returns
  409 Conflict, and the balance stays the same.
- maxmustermann recorded an expense of 50.00 € at 23:59 in Europe/Berlin. An expense of 50.00 € by maxmustermann at
  00:00 of the next day raises the balance.
- With 100.00 € configured for the daily limit, an expense of 100.00 € by maxmustermann raises the balance, and
  recording 100.01 € by maxmustermann returns 409 Conflict. Coffee Kitty does not start with 100.01 € configured.
- The admin jane_doe has recorded no expense today. An own expense of 50.00 € by jane_doe raises the balance of jane_doe
  by 50.00 €, and a further own expense of 0.01 € by jane_doe today returns 409 Conflict, because the daily limit also
  applies to an admin. Recording an own expense of 1,000.01 € by jane_doe returns 400 Bad Request.
- Bean purchases of 100 g and of 1,000 g raise the balance, and recording a bean purchase of 99 g or of 1,001 g returns
  400 Bad Request. Recording a bean purchase without a bean or without a weight, or an other expense with a bean or with
  a weight, returns 400 Bad Request. An expense with a note of 500 characters and a bean purchase with a name of 200
  characters raise the balance, and a note of 501 characters or a name of 201 characters returns 400 Bad Request. After
  each of these requests that returns 400 Bad Request, the balance stays the same.
- No bean has the name "Ethiopia Sidamo". A bean purchase of "Ethiopia Sidamo" creates the bean. After a second bean
  purchase of "ethiopia sidamo", Coffee Kitty still has one bean with this name, and both bean purchases refer to it.
- An expense does not change the kitty balance.

## Verification Method

Test: unit tests of the limits for the amount, the weight, the name of the bean, and the note, of the daily limit with a
fixed clock, of the rules for bean purchases and other expenses, and of the raised balance. Further tests complement
them: integration tests of the stored expense, bean, and balance, system tests of the status codes, and a Cucumber
scenario of the first success criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
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
  themselves. The 1,000 € are a hard upper limit for a single amount, and the weight limits are plausibility checks: A
  bag of beans weighs at least 100 g, and a weight above 1,000 g is more likely a typing error than one bag, such as a
  weight in kilograms that someone enters as grams. A user who buys more records several bean purchases.
- Note on this input: The daily limit is the only limit on the expenses of one day. The limit of 1,000.00 € for a single
  amount and the limits of the weight apply as well.
- This requirement covers only an expense that a user records for themselves. Coffee Kitty cannot check with whose money
  the user paid, so its private share is its full amount, and its kitty share is 0.00 € (GLOSSARY.md).
- Depends on FR-14 for the users and the admins. FR-12, FR-15, and FR-19 depend on this requirement for the beans that
  users rate, the balance that an expense raises, and the rules for an expense that an admin records for a user.
- Design, not part of this requirement: Coffee Kitty stores every time in UTC, so the check of the daily limit has to
  convert the time of an expense to Europe/Berlin.
- TBD (resolved): Is there a daily limit for the expenses that users record themselves? Yes, at most 5 expenses and
  50.00 € per day, configurable up to 100.00 €. The coffee app of SE@UHD allowed 10 expenses until this decision.
  Decided by: group lead (2026-11-25).
- TBD (resolved): How does a user who is not an admin record an expense beyond the daily limit, for example for a spare
  part of the coffee machine? An admin records it for the user (FR-19), as in the coffee app of SE@UHD. Decided by:
  group lead (2026-11-25).
- TBD (resolved): Does the daily limit apply to an admin who records an own expense? Yes. An admin records an expense
  beyond it through FR-19, also for themselves, as in the coffee app of SE@UHD. Until 2026-11-26, this requirement
  exempted every admin from the daily limit. Decided by: group lead (2026-11-26).
- TBD (open): Can users change or delete their own expenses? Contact person: team assistant.
- TBD (open): How does Coffee Kitty record the cash that the team assistant pays a user from the kitty for a positive
  balance? A deposit cannot be negative (FR-4), and a kitty adjustment belongs to no user (GLOSSARY.md). Contact person:
  team assistant.
- TBD (open): How does Coffee Kitty make sure that a user records an expense only for themselves? Coffee Kitty has no
  authentication yet. Contact person: group lead.
