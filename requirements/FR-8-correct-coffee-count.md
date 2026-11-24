# FR-8: Correct a Coffee Count

## Statement

When an admin corrects the coffee count of a user, Coffee Kitty MUST store the corrected coffee count only together with
the admin, the coffee count before the correction, and a note with the reason for the correction.

## Rationale

After the grace period of FR-3, a user cannot undo a coffee, and a wrong coffee count remains recorded, for example
after a double tap that the user notices too late. An admin can correct a wrong coffee count at any time, and the note
records why the coffee count changed.

## Success Criteria

- The admin jane_doe corrects the coffee count of maxmustermann from 3 to 2 with the note "Double tap on 16.11." The
  count is 2, and the corrections of maxmustermann list the count before (3), the count after (2), the note, and
  jane_doe.
- A correction by maxmustermann, who is not an admin, returns 403 Forbidden, and the count stays 3.
- A correction without a note or with a blank note returns 400 Bad Request, and the count stays 3.
- A correction to a negative count returns 400 Bad Request, and the count stays 3.
- The user maxmustermann has two coffees from 09:00:00 and adds a coffee at 10:00:00 (count 3). The admin jane_doe
  corrects the count to 2 at 10:02:00. An undo by maxmustermann at 10:03:00 returns 409 Conflict, and the count stays
  2, because the correction removed the coffee from 10:00:00 and the grace period of the coffees from 09:00:00 has
  passed.

## Verification Method

Test: Cucumber scenarios with a fixed clock, against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Business requirement (parent), decided by the group lead: "From now on, an admin can correct a wrong coffee count at
  any time and has to add a note with the reason for the correction."
- Depends on FR-3: A correction applies after the grace period of FR-3, and the last success criterion uses the undo.
  This requirement resolves the TBD of FR-3 on who is allowed to correct a coffee count after the grace period.
- FR-11 has an open TBD on the amount by which a correction changes the balance of the user.
- Verified by: correction.feature.
- The last success criterion follows the coffee app of SE@UHD, which removes the newest coffees of the user when a
  correction lowers the coffee count.
- TBD (open): Who is allowed to read the notes of corrections? Contact person: group lead.
- TBD (open): Is an admin allowed to correct a coffee count to its current value? Contact person: group lead.
- TBD (open): Is there an upper limit for a corrected coffee count? Contact person: group lead.
- TBD (open): What is the maximum length of a note? Contact person: group lead.
- Until the group lead answers these TBDs, Coffee Kitty lets only admins read the corrections, stores a correction to
  the current value, refuses a corrected coffee count above 1,000,000, and refuses a note with more than 500
  characters.
