# FR-5: Charge the Price per Cup

## Statement

When a user adds a coffee, Coffee Kitty MUST lower the user's balance by the price per cup that is stored at that time.

## Rationale

The users pay for their coffees through their balances. Coffee Kitty counts the coffees but does not charge them yet,
so a balance contains only deposits (FR-4) and expenses (FR-6) and does not show what a user owes.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 10.00 €. After maxmustermann adds a coffee, the
  balance is 9.50 €.
- With a balance of 0.00 €, a coffee lowers the balance to -0.50 €. Coffee Kitty does not refuse a coffee because of
  the balance.
- The price per cup changes from 0.50 € to 0.60 €. The change does not alter the balance of any user. A coffee that
  maxmustermann adds after the change lowers the balance by 0.60 €.
- A coffee does not change the kitty balance, because no cash moves.
- A request to add a coffee that returns 400 Bad Request or 404 Not Found does not change any balance.

## Verification Method

Test: unit tests of the lowered balance with a mocked price, complemented by integration tests of the stored balance and
of the stored price of a coffee, system tests of the refused requests, and a Cucumber scenario of the first and the
third success criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Follows from the definitions of the price per cup and of the balance in GLOSSARY.md: A coffee lowers the balance of
  its user by the price per cup at the time of the coffee, and a new price applies only to the coffees added after the
  change.
- Depends on the balance of a user (GLOSSARY.md) and on a stored price per cup.
- Related: FR-5b, which specifies the balance after an undo. For FR-5b, Coffee Kitty has to store the price that each
  coffee was charged. The migration V12 adds the column `coffee_times.price_cents` for this price.
- TBD (open): Who sets the price per cup, and with which request? GLOSSARY.md states that an admin sets it, and no
  requirement specifies the request yet. Contact person: group lead.
- TBD (open): What does a coffee cost while no price is stored? No migration stores a price. Contact person: group
  lead.
