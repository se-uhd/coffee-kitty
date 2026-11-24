# FR-11: Credit an Undone Coffee

## Statement

When a user undoes a coffee, Coffee Kitty MUST raise the user's balance by the amount by which that coffee lowered it.

## Rationale

An undo takes back a coffee that a user added by mistake (FR-3), so the balance after the undo has to equal the balance
before the coffee. The price per cup can change between a coffee and its undo. If the undo raised the balance by the
price at the time of the undo, a user would gain 0.10 € after a rise of the price from 0.50 € to 0.60 €.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. maxmustermann adds a coffee at 10:00:00
  (balance 1.50 €) and undoes it at 10:02:00. The balance is 2.00 €.
- The price per cup is 0.50 €, and the balance of maxmustermann is 2.00 €. maxmustermann adds a coffee at 10:00:00
  (balance 1.50 €). The price per cup changes to 0.60 € at 10:01:00. maxmustermann undoes the coffee at 10:02:00. The
  balance is 2.00 €, not 2.10 €.
- An undo that returns 409 Conflict (FR-3) does not change the balance.

## Verification Method

Test: unit tests of the raised balance with a fixed clock, complemented by integration tests of the stored price of a
coffee and a Cucumber scenario of the second success criterion. All tests except the unit tests run against PostgreSQL
in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Depends on FR-3: FR-3 specifies when an undo lowers the coffee count and when it returns 409 Conflict.
- Depends on FR-5: FR-5 specifies the amount by which a coffee lowers the balance.
- Coffee Kitty has to store the price of each coffee that a user can still undo. The migration V12 adds the column
  `coffee_times.price_cents` for this price. Until V12, the table stored only the time of a coffee.
- TBD (open): By which amount does a correction (FR-8) change the balance? When a user adds a coffee, Coffee Kitty
  drops the stored times of the coffees outside their grace period, and with V12 also their prices. A correction that
  removes such a coffee cannot use the price of that coffee. Contact person: group lead.
