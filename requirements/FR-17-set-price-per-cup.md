# FR-17: Set the Price per Cup

## Statement

When an admin sets the price per cup, Coffee Kitty MUST charge that price for each coffee that a user adds afterwards.

Coffee Kitty has one price per cup, which is the same for every user. The price per cup of a new installation is 0.50 €,
and a price per cup of 0.00 € is allowed. Coffee Kitty MUST report the price per cup to a user who requests it.

Coffee Kitty MUST refuse a price that a user who is not an admin sets with 403 Forbidden and a negative price with 400
Bad Request. A refused request MUST NOT change the price per cup.

Coffee Kitty MUST refuse a price above 1,000.00 € with 400 Bad Request. This limit protects against a typing error and
is far above a real price per cup.

## Rationale

The price of the beans changes, and the group wants the coffees to pay for the beans. FR-5 needs a price per cup before
the first user adds a coffee.

## Success Criteria

- On a new installation, the price per cup is 0.50 €.
- The admin jane_doe sets the price per cup to 0.60 €. The price per cup is 0.60 €, and a coffee that maxmustermann adds
  afterwards lowers the balance of maxmustermann by 0.60 € (FR-5).
- Coffee Kitty accepts a price per cup of 0.00 € and a price per cup of 1,000.00 €. A request to set the price per cup
  to -0.01 € or to 1,000.01 € returns 400 Bad Request, and the price per cup stays the same.
- A request by maxmustermann, who is not an admin, to set the price per cup returns 403 Forbidden, and the price per cup
  stays the same.

## Verification Method

Test: unit tests of the limits and the role check. Further tests complement them: integration tests of the stored price,
system tests of the status codes, and a Cucumber scenario of the second success criterion. All tests except the unit
tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "There is one price per cup for everybody. It starts at 0.50 €, and an admin can
  change it. It can also be set to 0." "A single amount, the price included, is at most 1,000 €." "Users see their own
  balance and the current price per cup."
- Input, from the group lead (2026-11-25): The 1,000 € are a hard upper limit for a single amount, also for the price
  per cup. The limit protects against a typing error of the admin who sets the price.
- Depends on FR-14: FR-14 specifies which users are admins.
- FR-5 and FR-18 depend on this requirement: A coffee lowers the balance by the price per cup (FR-5), and a correction
  changes the balance by the price per cup at the time of the correction (FR-18).
- TBD (open): Does Coffee Kitty keep the earlier prices? In the coffee app of SE@UHD, "every price change is kept in a
  price history that admins can look at". Until the team assistant answers this question, this requirement specifies
  only the current price per cup, and Coffee Kitty stores no earlier price. Contact person: team assistant.
