# FR-5: Pay for a Coffee

## Statement

When a user adds a coffee, Coffee Kitty MUST lower the user's balance by the price per cup.

## Rationale

Each coffee costs the same price. Users want to see what they owe.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. After maxmustermann adds a coffee, the
  balance is 1.50 €.
- With a balance of 0.00 €, a coffee lowers the balance to -0.50 €.
- The price per cup changes from 0.50 € to 0.60 €. The change does not alter the balance of maxmustermann. A coffee
  that maxmustermann adds after the change lowers the balance by 0.60 €.
- A coffee does not change the kitty balance.

## Verification Method

Test: unit tests of the lowered balance, complemented by integration tests of the stored balance, system tests of the
status codes, and a Cucumber scenario of the first success criterion. All tests except the unit tests run against
PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Business requirement (parent): "Our group wants each member to pay for the coffees they drink, so that we can buy new
  beans with the money in the kitty."
- User requirement (parent): "I want to add a coffee with one tap and see how much I owe the kitty."
- Depends on the balance of a user and on the price per cup (GLOSSARY.md). A coffee is not a payment and moves no cash
  (GLOSSARY.md).
- FR-11 depends on this requirement: An undo raises the balance by the amount by which the coffee lowered it.
- TBD (resolved): Does a price change apply to coffees added before it? No. A new price applies only to the coffees
  added after the change (GLOSSARY.md, price per cup).
- TBD (open): Can a balance go below zero, and is there a limit? GLOSSARY.md defines a negative balance as money that
  the user owes the kitty. No source sets a limit. Contact person: group lead.
