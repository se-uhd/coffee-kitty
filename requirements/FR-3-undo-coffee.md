# FR-3: Undo a Coffee

## Statement

When a user undoes their most recent coffee within the grace period of 5 minutes, Coffee Kitty MUST lower the user's
coffee count by one.

Each coffee has its own grace period, which starts at the instant at which the user adds that coffee. The instant at
which the grace period ends belongs to the grace period. After an undo, the coffee that the user added before the undone
coffee is the most recent coffee.

Coffee Kitty MUST refuse an undo after the grace period and an undo at a coffee count of 0 with 409 Conflict. A refused
undo MUST NOT change the coffee count.

## Rationale

Users sometimes add a coffee by mistake, for example with a double tap. Without an undo, they cannot lower their coffee
count, and a wrong coffee count remains recorded. Assumption: 5 minutes are enough to notice a mistake.

## Success Criteria

- An undo at 10:05:00 of a coffee added at 10:00:00 lowers the count from 3 to 2.
- An undo at 10:05:01 of a coffee added at 10:00:00 returns 409 Conflict, and the count stays 3.
- With a coffee count of 0, an undo returns 409 Conflict, and the count stays 0.
- With coffees added at 09:00:00, 09:00:00, and 10:00:00, an undo at 10:01:00 lowers the count from 3 to 2. A second
  undo at 10:02:00 returns 409 Conflict, because the coffees from 09:00:00 are outside their grace period. The count
  stays 2.
- With coffees added at 09:00:00, 09:00:00, 10:00:00, and 10:01:00, undos at 10:02:00 and 10:03:00 lower the count from
  4 to 2.
- With coffees added at 09:00:00, 09:00:00, 10:00:00, and 10:04:00, an undo at 10:06:00 lowers the count from 4 to 3. A
  second undo at 10:06:00 returns 409 Conflict, because the coffee from 10:00:00 is outside its grace period. The count
  stays 3.
- An undo for an unknown user returns 404 Not Found.

## Verification Method

Test: Cucumber scenarios with a fixed clock and a system test of the unknown user, against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- User requirement (parent): "If I tap twice by mistake, I want to fix it myself."
- Input, from the team assistant: "A user can undo their own most recent coffee within 5 minutes of recording it. We
  call these 5 minutes the grace period. Exactly 5 minutes still counts as within the grace period."
- Changes the rule that nothing lowers the coffee count (README.md and GLOSSARY.md of Coffee Kitty 0.1.0).
- Depends on FR-13: FR-13 specifies how a user adds the coffee that an undo takes back.
- FR-8, FR-9, FR-11, and FR-12 depend on this requirement. A correction (FR-8) changes which coffee is the most recent
  coffee. FR-9 reports whether an undo is possible. FR-11 specifies the balance after an undo. An undo removes the
  rating of the undone coffee (FR-12).
- Design, not part of this requirement: The 5 minutes are the default of the setting
  `campus-coffee.consumption.grace-period`. The success criteria apply to this default.
- Verified by: consumption.feature, the Scenario Outline "Undo a coffee within the grace period" and the scenarios "Undo
  at a coffee count of 0", "Undo twice in a row when only the most recent coffee is within its grace period", "Undo two
  recent coffees in a row", and "Undo twice in a row when the grace period of the coffee before has passed".
  `ConsumptionSystemTests` checks the unknown user.
- TBD (resolved): What does a second undo in a row do? After an undo, the user can undo the coffee before if that coffee
  is still within its own grace period, as in the coffee app of SE@UHD (the fourth, fifth, and sixth success criterion).
  Decided by: group lead.
- TBD (open): Are 5 minutes enough to notice a mistake? Contact person: group lead.
- TBD (resolved): Who is allowed to correct a coffee count after the grace period? An admin, with a note (FR-8). Decided
  by: group lead.
- TBD (open): How does Coffee Kitty make sure that a user undoes only their own coffee? Coffee Kitty has no
  authentication yet. Until the group lead answers this question, this requirement lets every client undo the most
  recent coffee of every user. Contact person: group lead.
