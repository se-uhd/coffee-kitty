# FR-5b: Credit an Undone Coffee

## Statement

When a user undoes a coffee, Coffee Kitty MUST raise the user's balance by the amount by which that coffee lowered it.

## Rationale

An undo takes back a coffee that a user added by mistake (FR-3), so the balance after the undo has to equal the balance
before the coffee. The price per cup can change between a coffee and its undo. If the undo credited the price at the
time of the undo, the two balances would differ: After a rise of the price from 0.50 € to 0.60 €, a user who undoes a
coffee from before the rise would gain 0.10 €.

## Success Criteria

- The price per cup is 0.50 €, and the balance of maxmustermann is 10.00 €. maxmustermann adds a coffee at 10:00:00
  (balance 9.50 €) and undoes it at 10:02:00. The balance is 10.00 €.
- The price per cup is 0.50 €, and the balance of maxmustermann is 10.00 €. maxmustermann adds a coffee at 10:00:00
  (balance 9.50 €). The price per cup changes to 0.60 € at 10:01:00. maxmustermann undoes the coffee at 10:02:00. The
  balance is 10.00 €, not 10.10 €.
- The price per cup is 0.50 €, and the balance of maxmustermann is 10.00 €. maxmustermann adds a coffee at 10:00:00,
  the price per cup changes to 0.60 € at 10:01:00, and maxmustermann adds a second coffee at 10:01:30 (balance 8.90 €).
  An undo at 10:02:00 raises the balance to 9.50 €, and a second undo at 10:03:00 raises it to 10.00 €.
- An undo that returns 409 Conflict (FR-3) does not change the balance.
- An undo does not change the kitty balance.

## Verification Method

Test: unit tests of the raised balance with a fixed clock, complemented by integration tests of the stored price of a
coffee and a Cucumber scenario of the second success criterion. All tests except the unit tests run against PostgreSQL
in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Depends on FR-3, which specifies when an undo lowers the coffee count, and on FR-5, which specifies the amount by
  which a coffee lowers the balance.
- Coffee Kitty has to store the price that it charged for each coffee that a user can still undo. The table
  `coffee_times` holds the time of each of these coffees, and the migration V12 adds the column `price_cents` to it.
  Until V12, the table had no column for the price, and an undo could credit only the price at the time of the undo.
- TBD (open): By which amount does a correction (FR-8) change the balance? When a user adds a coffee, Coffee Kitty
  drops the stored times of the coffees outside their grace period, and with V12 also their prices. A correction that
  removes such a coffee therefore cannot credit the price that the coffee was charged. Proposal: A correction credits
  the stored price of a removed coffee if Coffee Kitty still stores it, and the price per cup at the time of the
  correction otherwise. For each coffee that it adds, it charges the price per cup at the time of the correction.
  Contact person: group lead.
