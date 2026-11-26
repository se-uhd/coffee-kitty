# FR-8: Correct a Coffee Count

## Statement

When an admin corrects the coffee count of a user, Coffee Kitty MUST store the corrected coffee count together with a
record of the correction.

The record of a correction holds the admin, the coffee count before the correction, the note with the reason for the
correction if the request has one, and the time of the correction. Coffee Kitty MUST store the corrected coffee count
and the record together or not at all. A correction to a lower coffee count removes the most recent coffees of the
user. A correction to a higher coffee count adds coffees that the user cannot undo, and the coffees that the user
added stay undoable within their grace period.

Coffee Kitty MUST list the corrections of a user for an admin, newest first.

A request MAY contain a note, and a blank note counts as no note. A blank note contains only whitespace, which includes
the no-break space. A request MAY contain the coffee count that the admin saw before the correction (`expectedCount`).

Coffee Kitty MUST refuse a correction by a user who is not an admin with 403 Forbidden. Coffee Kitty MUST refuse a
correction to a negative coffee count or to a coffee count above 1,000,000, and a correction with a note longer than
500 characters, with 400 Bad Request. Coffee Kitty MUST refuse a correction with an expected coffee count that differs
from the coffee count with 409 Conflict. A refused correction MUST NOT change the coffee count.

## Rationale

After the grace period of FR-3, a user cannot undo a coffee, and a wrong coffee count remains recorded, for example
after a double tap that the user notices too late. A correction changes what a user owes the group. The group therefore
has to be able to check later who changed a coffee count, from which value, and why.

## Success Criteria

- With a coffee count of 3 for maxmustermann, the admin jane_doe corrects the coffee count to 2 with the note "Double
  tap on 16.11." The count is 2, and the corrections of maxmustermann list the count before (3), the count after (2),
  the note, jane_doe, and the time of the correction.
- After jane_doe corrects the same coffee count a second time, the corrections of maxmustermann list the second
  correction first.
- With a coffee count of 3, a request by maxmustermann, who is not an admin, to correct the coffee count to 2 returns
  403 Forbidden, and the count stays 3. The request also returns 403 Forbidden when its note has 501 characters.
- With a coffee count of 3, a correction to 2 without a note or with a blank note sets the count to 2, and the
  corrections of maxmustermann list the correction without a note.
- With a coffee count of 3, a correction with a note of 501 characters returns 400 Bad Request, and the count stays 3.
  A correction with a note of 500 characters sets the count.
- With a coffee count of 3, a correction to a negative count or to 1,000,001 returns 400 Bad Request, and the count
  stays 3. A correction to 1,000,000 sets the count.
- With a coffee count of 3, a correction to 1 with the expected count 3 sets the count to 1. After maxmustermann adds a
  coffee (count 4), a correction to 1 with the expected count 3 returns 409 Conflict, and the count stays 4.
- The user maxmustermann has two coffees from 09:00:00 and adds a coffee at 10:00:00 (count 3). The admin jane_doe
  corrects the count to 2 at 10:02:00. An undo by maxmustermann at 10:03:00 returns 409 Conflict, and the count stays 2,
  because the correction removed the coffee from 10:00:00 and the grace period of the coffees from 09:00:00 has passed.
- The user maxmustermann has a coffee from 09:00:00 and adds a coffee at 10:00:00 (count 2). The admin jane_doe corrects
  the count to 4 at 10:01:00. An undo by maxmustermann at 10:02:00 lowers the count to 3. A second undo at 10:03:00
  returns 409 Conflict, and the count stays 3, because a user cannot undo a coffee that a correction added.
- If Coffee Kitty cannot store the correction, the request fails, and the count stays the same.
- A correction of the coffee count of an unknown user returns 404 Not Found, and a correction without a known acting
  user returns 401 Unauthorized.
- With a coffee count of 3, a correction to 3 with a note returns the count 3, and the corrections of the user list the
  correction (see the second open TBD).
- A request by maxmustermann, who is not an admin, to list the corrections of a user returns 403 Forbidden (see the
  first open TBD).

## Verification Method

Test: Cucumber scenarios with a fixed clock and system tests, against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Business requirement (parent), decided by the group lead: "From now on, an admin can correct a wrong coffee count at
  any time and has to add a note with the reason for the correction." The group lead changed the required note to an
  optional note on 2026-11-26 (see the TBDs below).
- Input, from the team assistant: "Notes have at most 500 characters."
- Depends on FR-3: Only a correction removes a coffee whose grace period of FR-3 has passed, and a correction changes
  which coffee an undo applies to. This requirement resolves the TBD of FR-3 on who is allowed to correct a coffee count
  after the grace period.
- Depends on FR-13 and FR-14: FR-13 specifies the coffees that a user adds, and FR-14 specifies which users are admins.
- FR-9, FR-12, and FR-18 depend on this requirement. The undo status (FR-9) follows the undo after a correction. A
  correction removes the ratings of the coffees that it removes (FR-12). FR-18 specifies the balance after a correction.
- Verified by: correction.feature for the first, third, fourth, sixth, eighth, and ninth success criterion,
  `CorrectionSystemTests` for the other criteria, and `FailureSystemTests` for the correction that Coffee Kitty cannot
  store.
- The rule that a correction to a lower coffee count removes the most recent coffees follows the coffee app of SE@UHD.
- TBD (resolved): What is the maximum length of a note? 500 characters. Decided by: team assistant.
- TBD (resolved): Does a correction need a note? No. Until 2026-11-26, this requirement refused a correction without a
  note, and the coffee app of SE@UHD accepted it. Decided by: group lead (2026-11-26), as in the coffee app.
- TBD (resolved): How does an admin avoid a correction based on a count that changed meanwhile? With the expected count,
  as in the coffee app of SE@UHD. RFC 9110 names a conflict with an earlier change as an example of 409 Conflict, and
  412 Precondition Failed belongs to the conditional headers, such as `If-Match`. Decided by: group lead (2026-11-26).
- TBD (open): Who is allowed to read the corrections of a user, including the notes? Until the group lead answers this
  question, this requirement lets only an admin read the corrections. Contact person: group lead.
- TBD (open): Is an admin allowed to correct a coffee count to its current value? Until the group lead answers this
  question, this requirement accepts a correction to the current value, and Coffee Kitty stores the correction. Contact
  person: group lead.
- TBD (resolved): Is there an upper limit for a corrected coffee count? Yes, 1,000,000, the limit of the coffee app of
  SE@UHD for a correction, far above any count of a coffee group. Until 2026-11-26, this requirement set no limit of its
  own, and a correction reached 2,147,483,647, the largest count of FR-13. Decided by: group lead (2026-11-26).
