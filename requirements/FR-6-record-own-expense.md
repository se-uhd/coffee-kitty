# FR-6: Record Own Expenses

## Statement

When a user records an expense between 0.01 € and 1,000.00 € that they paid with their own money, Coffee Kitty MUST
raise the user's balance by the expense amount.

## Rationale

Users buy beans and other supplies for the group with their own money. Today, they hand in receipts, and the team
assistant pays back the money from the kitty or forgets the receipts.

## Success Criteria

- The balance of maxmustermann is 0.00 €. After maxmustermann records a bean purchase of 12.99 € for 500 g of beans, the
  balance is 12.99 €. After maxmustermann records another expense, 4.50 € for filters, the balance is 17.49 €.
- An expense of 0.01 € raises a balance of 0.00 € to 0.01 €, and an expense of 1,000.00 € raises a balance of 0.00 € to
  1,000.00 €.
- A request to record an expense of 0.00 € or of 1,000.01 € returns 400 Bad Request, and the balance stays the same.
- Bean purchases of 100 g and of 1,000 g raise the balance. A request to record a bean purchase of 99 g or of 1,001 g
  returns 400 Bad Request, and the balance stays the same.
- A request to record a bean purchase without the name of the beans or without a weight returns 400 Bad Request, and the
  balance stays the same.
- A request to record any other expense with the name of beans or with a weight returns 400 Bad Request, and the balance
  stays the same.
- An expense with a note of 500 characters raises the balance. A request to record an expense with a note of 501
  characters returns 400 Bad Request, and the balance stays the same.

## Verification Method

Test: unit tests of the limits for the amount, the weight, and the note, the rules for bean purchases and other
expenses, and the raised balance, complemented by integration tests of the stored expense and the stored balance, system
tests of the status codes, and a Cucumber scenario of the first success criterion. All tests except the unit tests run
against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "When someone buys something for the group with their own money, such as a bag of
  beans, they record it as an expense."
- Input, from the team assistant: A bean purchase has the name of the beans and a weight from 100 g to 1,000 g. Any other
  expense, such as filters or milk, has neither. A note is optional and has at most 500 characters.
- The private share of the expense is its full amount, and the kitty share is 0 (GLOSSARY.md).
- Depends on the balance of a user (GLOSSARY.md).
- TBD (open): Can users correct or delete their own expenses? Contact person: team assistant.
- TBD (open): Is there a daily limit for the expenses that users record themselves? Contact person: team assistant.
