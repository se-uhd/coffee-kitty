# FR-18: Charge or Credit a Correction

## Statement

When an admin corrects the coffee count of a user, Coffee Kitty MUST change the user's balance by the price per cup for
each coffee that the correction adds or removes.

The price per cup at the time of the correction applies. A correction to a higher coffee count lowers the balance, and a
correction to a lower coffee count raises the balance. Coffee Kitty MUST change the balance by one amount for the whole
difference between the two coffee counts. A correction that Coffee Kitty refuses (FR-8) MUST NOT change the balance. A
correction MUST NOT change the kitty balance.

## Rationale

A coffee lowers the balance by the price per cup (FR-5). A correction changes the coffee count without a coffee, so the
balance would no longer fit the coffee count. Coffee Kitty does not store the price of a coffee after its grace period,
so a correction cannot use the price that each removed coffee had.

## Success Criteria

- The price per cup is 0.50 €, and maxmustermann has a coffee count of 3 and a balance of -1.50 €. The admin jane_doe
  corrects the coffee count to 5. The balance is -2.50 €.
- The price per cup is 0.50 €, and maxmustermann has a coffee count of 3 and a balance of -1.50 €. The price per cup
  changes to 0.60 €. The admin jane_doe corrects the coffee count to 2. The balance is -0.90 €.
- A correction of a coffee count to its current value does not change the balance.
- A correction that returns 400 Bad Request or 403 Forbidden (FR-8) does not change the balance.
- A correction does not change the kitty balance.

## Verification Method

Test: unit tests of the changed balance. Further tests complement them: integration tests of the stored balance and a
Cucumber scenario of the second success criterion. All tests except the unit tests run against PostgreSQL in
Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "A corrected total is charged or credited at the current price, as one amount for the
  whole difference."
- Depends on FR-8: FR-8 specifies the correction and which requests Coffee Kitty refuses.
- Depends on FR-5 and FR-17: FR-5 specifies the amount by which a coffee lowers the balance, and FR-17 specifies the
  price per cup.
- FR-15 depends on this requirement: FR-15 reports the balance that a correction changes.
- This requirement resolves the TBD of FR-11 on the amount by which a correction changes the balance. An undo (FR-11)
  raises the balance by the price that the undone coffee had. A correction uses the price per cup at the time of the
  correction.
