# FR-3: Undo a Coffee

## Statement

When a user undoes their most recent coffee within 5 minutes of adding it, Coffee Kitty MUST lower the user's coffee
count by one.

## Rationale

Users sometimes add a coffee by mistake, for example with a double tap. Coffee Kitty cannot yet lower a coffee count,
so a wrong entry remains recorded. Assumption: 5 minutes are enough to notice a mistake.

## Success Criteria

- A coffee added at 10:00:00 and undone at 10:05:00 lowers the count from 3 to 2.
- An undo at 10:05:01 returns 409 Conflict, and the count stays 3.
- With a coffee count of 0, an undo returns 409 Conflict, and the count stays 0.
- With coffees added at 09:00:00, 09:00:00, and 10:00:00, an undo at 10:01:00 lowers the count from 3 to 2, and a
  second undo at 10:02:00 returns 409 Conflict, because the coffee from 09:00:00 is outside its grace period. The count
  stays 2.
- With coffees added at 09:00:00, 09:00:00, 10:00:00, and 10:01:00, undos at 10:02:00 and 10:03:00 lower the count
  from 4 to 2.

## Verification Method

Test: Cucumber scenarios with a fixed clock, against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- User requirement (parent): "If I tap twice by mistake, I want to fix it myself."
- Changes the rule that the coffee count is never reduced (README.md, GLOSSARY.md).
- Verified by: consumption.feature, the Scenario Outline "Undo a coffee within the grace period" and the four scenarios
  after it.
- The last two success criteria follow the coffee app of SE@UHD, which undoes the coffee before after an undo if that
  coffee is still within its own grace period.
- TBD (open): Are 5 minutes enough to notice a mistake? Contact person: group lead.
- TBD (open): Who is allowed to correct a coffee count after the grace period? Contact person: group lead.
