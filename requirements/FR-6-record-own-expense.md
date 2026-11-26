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
Kitty SHOULD refuse a bean purchase with a weight below 100 g or above 1,000 g with 400 Bad Request. A refused expense
MUST NOT change the balance. An expense MUST NOT change the kitty balance.

A user who is not an admin has a daily limit: at most 5 expenses and at most 50.00 € in total on one day. The daily
limit counts every expense of the user on that day, also an expense that an admin recorded for the user (FR-19). Coffee
Kitty MUST refuse an expense that exceeds the daily limit with 409 Conflict, and the message of the response MUST state
that an admin can record the expense. A day starts at midnight in the time zone Europe/Berlin. The group MAY configure
another amount for the daily limit, and Coffee Kitty MUST NOT start with a configured amount above 100.00 €. The daily
limit does not apply to an admin.

## Rationale

Users buy beans and other supplies for the group with their own money. Today, they hand in receipts, and the team
assistant pays back the money from the kitty or forgets the receipts.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After maxmustermann records a bean purchase of 12.99 € for 500 g of the bean
  "Ethiopia Sidamo", the balance is 12.99 €, and Coffee Kitty has stored the expense with the bean and the weight. After
  maxmustermann records an other expense of 4.50 € with the note "Filters", the balance is 17.49 €.
- An expense of 0.01 € raises a balance of 0.00 € to 0.01 €. Recording an expense of 0.00 € returns 400 Bad Request.
- maxmustermann, who is not an admin, has recorded no expense today. An expense of 50.00 € raises the balance by
  50.00 €. Recording a further expense of 0.01 € today returns 409 Conflict with a message that names an admin.
- maxmustermann has recorded expenses of 30.00 € and 19.99 € today. Recording an expense of 0.02 € returns 409 Conflict,
  and an expense of 0.01 € raises the balance.
- maxmustermann has recorded 5 expenses of 1.00 € today. Recording a sixth expense of 1.00 € returns 409 Conflict.
- maxmustermann recorded an expense of 50.00 € at 23:59 in Europe/Berlin. An expense of 50.00 € by maxmustermann at
  00:00 of the next day raises the balance.
- With 100.00 € configured for the daily limit, an expense of 100.00 € by maxmustermann raises the balance, and
  recording an expense of 100.01 € returns 409 Conflict. Coffee Kitty does not start with 100.01 € configured.
- An own expense of 1,000.00 € by the admin jane_doe raises the balance of jane_doe by 1,000.00 €, because the daily
  limit does not apply to an admin. Recording an expense of 1,000.01 € by jane_doe returns 400 Bad Request.
- Bean purchases of 100 g and of 1,000 g raise the balance, and recording a bean purchase of 99 g or of 1,001 g returns
  400 Bad Request. Recording a bean purchase without a bean or without a weight, or an other expense with a bean or with
  a weight, returns 400 Bad Request. An expense with a note of 500 characters and a bean purchase with a name of 200
  characters raise the balance, and a note of 501 characters or a name of 201 characters returns 400 Bad Request.
- No bean has the name "Ethiopia Sidamo". A bean purchase of "Ethiopia Sidamo" creates the bean. After a second bean
  purchase of "ethiopia sidamo", Coffee Kitty still has one bean with this name, and both bean purchases refer to it.
- A refused request does not change the balance, and an expense does not change the kitty balance.

## Verification Method

Test: unit tests of the limits for the amount, the weight, the name of the bean, and the note, of the daily limit with a
fixed clock, of the kinds of expense, and of the raised balance. Further tests complement them: integration tests of the
stored expense, bean, and balance, system tests of the status codes, and a Cucumber scenario of the first success
criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: someone who buys something for the group with their own money records an expense.
- Input, from the team assistant: "between 100 g and 1,000 g", "between 0.01 € and 1,000 €", a note "of up to 500
  characters", bean names "have at most 200 characters", "at most 50 € in total per day", "midnight, German time".
- Input, from the group lead (2026-11-25): The 1,000 € are a hard limit, and the weight limits are plausibility checks.
- Depends on FR-14 for the users and the admins. FR-12, FR-15, and FR-19 depend on this requirement for the beans that
  users rate, the balance that an expense raises, and the rules of an expense.
- TBD (resolved): Is there a daily limit for the expenses that users record themselves? Yes, 5 expenses, not the 10 of
  the team assistant, and 50.00 € per day, configurable up to 100.00 €. Decided by: group lead (2026-11-25).
- TBD (resolved): How does a user who is not an admin record an expense beyond the daily limit? An admin records it for
  the user (FR-19), as in the coffee app of SE@UHD. Decided by: group lead (2026-11-25).
- TBD (open): Can users change or delete their own expenses? Contact person: team assistant.
- TBD (open): How does Coffee Kitty record the cash that a user with a positive balance gets from the kitty? A deposit
  cannot be negative (FR-4), and a kitty adjustment belongs to no user. Contact person: team assistant.
- TBD (open): How does Coffee Kitty make sure that a user records an expense only for themselves? Coffee Kitty has no
  authentication yet. Contact person: group lead.
