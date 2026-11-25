# FR-5: Pay for a Coffee

## Statement

When a user adds a coffee, Coffee Kitty MUST lower the user's balance by the price per cup.

The price per cup at the time of the coffee applies. A balance can be negative, and a negative balance is the amount
that the user owes the kitty. A coffee MUST NOT change the kitty balance.

## Rationale

The group charges one price per cup, which is the same for every user, and wants each user to pay for their own coffees.
The balance records what a user owes the kitty.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. After maxmustermann adds a coffee, the
  balance is 1.50 €.
- With a price per cup of 0.50 € and a balance of 0.00 €, a coffee lowers the balance to -0.50 €.
- The price per cup is 0.50 €, and maxmustermann has added two coffees (balance -1.00 €). The price per cup changes to
  0.60 €. The balance stays -1.00 €. A coffee that maxmustermann adds after the change lowers the balance to -1.60 €.
- A coffee does not change the kitty balance.
- A request to add a coffee that Coffee Kitty refuses (FR-13) does not change the balance.

## Verification Method

Test: unit tests of the lowered balance. Further tests complement them: integration tests of the stored balance, system
tests of the balance after a user adds a coffee, and a Cucumber scenario of the first success criterion. All tests
except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Business requirement (parent): "Our group wants each member to pay for the coffees they drink, so that we can buy new
  beans with the money in the kitty."
- User requirement (parent): "I want to add a coffee with one tap and see how much I owe the kitty."
- Input, from the team assistant: "Every user has a balance in euros and cents. A coffee lowers the balance by its
  price." "A negative balance means that the user owes the kitty money."
- The title uses the words of the user requirement. In the terms of GLOSSARY.md, a coffee is not a payment and moves no
  cash.
- Depends on FR-13 and FR-17: FR-13 specifies how a user adds a coffee, and FR-17 specifies the price per cup and who
  sets it.
- FR-11, FR-15, and FR-18 depend on this requirement. An undo raises the balance by the amount by which the coffee
  lowered the balance (FR-11). FR-15 reports the balance. A correction changes the balance for each coffee that it adds
  or removes (FR-18).
- TBD (resolved): Does a price change apply to coffees added before the change? No. "A new price applies only to coffees
  recorded after the change. Every coffee keeps the price it had when it was recorded." Decided by: team assistant.
- TBD (resolved): Can a balance go below zero? Yes (second success criterion). Decided by: team assistant.
- TBD (open): Is there a lowest balance below which a user cannot add a coffee? Until the group lead answers this
  question, this requirement sets no lower limit for a balance. Contact person: group lead.
