# FR-13: Add a Coffee

## Statement

When a user adds a coffee, Coffee Kitty MUST raise the user's coffee count by one.

A request adds exactly one coffee. Coffee Kitty MUST count every coffee, also when several requests add a coffee for the
same user at the same instant. Coffee Kitty MUST refuse a request that adds another number of coffees with 400 Bad
Request and a request for an unknown user with 404 Not Found.

## Rationale

The coffee count replaces the tally sheet next to the coffee machine, on which everyone makes a stroke for each coffee.
The team assistant counts the strokes by hand once a month and makes mistakes.

## Success Criteria

- With a coffee count of 0, maxmustermann adds a coffee. The count is 1. After a second coffee, the count is 2.
- A coffee that maxmustermann adds does not change the coffee count of another user.
- Several requests add a coffee for maxmustermann at the same time. The coffee count rises by the number of requests.
- A request to add 2 coffees or 0 coffees returns 400 Bad Request, and the count stays the same.
- A request to add a coffee for an unknown user returns 404 Not Found.
- With a coffee count of 2,147,483,647, a request to add a coffee returns 409 Conflict, and the count stays the same.

## Verification Method

Test: Cucumber scenarios of the first success criterion, system tests of the other criteria, and unit tests, against
PostgreSQL in Testcontainers except for the unit tests.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Parent, from the team assistant: "What I want in the end: an app in which everyone records their own coffees and sees
  what they owe."
- Input, from the team assistant: "Adding a coffee raises the count by exactly one. If two people add a coffee for the
  same user at the same moment, both coffees count."
- Depends on FR-14: FR-14 creates the users and their coffee counts, which start at 0.
- FR-3, FR-5, FR-8, and FR-12 depend on this requirement. An undo takes back a coffee (FR-3). A coffee lowers the
  balance (FR-5). A correction adds or removes coffees (FR-8). A rating belongs to a coffee (FR-12).
- The last success criterion states a technical limit: 2,147,483,647 is the largest value of the PostgreSQL type
  `integer`, which stores the coffee count. Only a correction (FR-8) can set a coffee count this high.
- Verified by: consumption.feature, the scenarios "A user adds a coffee" and "A user adds two coffees",
  `ConsumptionSystemTests`, and `CorrectionSystemTests` for the largest coffee count.
- Coffee Kitty 0.1.0 had this function without a requirement. This requirement records it with a new ID.
- TBD (open): How does Coffee Kitty make sure that a user adds only their own coffees? Coffee Kitty has no
  authentication yet. Until the group lead answers this question, this requirement lets every client add a coffee for
  every user. Contact person: group lead.
