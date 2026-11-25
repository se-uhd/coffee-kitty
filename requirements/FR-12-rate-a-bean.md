# FR-12: Rate a Bean

## Statement

When a user rates a bean with a whole-number score from 1 to 5, Coffee Kitty MUST store one rating of that bean with
that score for the user's most recent coffee.

Each coffee has at most one rating. Until the user adds the next coffee, a further rating MUST replace the rating of the
most recent coffee, also when the user rates a different bean.

Coffee Kitty MUST refuse a rating at a coffee count of 0 with 409 Conflict, a rating with another score with 400 Bad
Request, and a rating of an unknown bean with 404 Not Found. A rating MUST NOT change a balance.

## Rationale

The group buys different beans. A user rates a bean right after adding a coffee, and the ratings of a bean show the
group which beans to buy again.

## Success Criteria

- The user maxmustermann adds a coffee and rates the bean "Ethiopia Sidamo" with a score of 4. The bean has one rating,
  with a score of 4. The user then rates the same bean with a score of 2 before adding another coffee. The bean has one
  rating, with a score of 2. The user adds a second coffee and rates the bean with a score of 5. The bean has two
  ratings, with the scores 2 and 5.
- The user maxmustermann adds a coffee and rates the bean "Ethiopia Sidamo" with a score of 4. The user then rates the
  bean "Brazil Santos" with a score of 3 before adding another coffee. "Ethiopia Sidamo" has no rating, and "Brazil
  Santos" has one rating, with a score of 3.
- With a coffee count of 0, a request to rate a bean returns 409 Conflict, and the bean has no rating.
- After maxmustermann adds a coffee, a request to rate a bean with a score of 0, 6, or 4.5 returns 400 Bad Request, and
  the bean has no rating.
- After maxmustermann adds a coffee, a request to rate an unknown bean returns 404 Not Found.
- The user maxmustermann adds a coffee at 10:00:00, rates the bean "Ethiopia Sidamo" with a score of 4, and undoes the
  coffee at 10:02:00. The bean has no rating.

## Verification Method

Test: unit tests of the limits for the score and of the rating of the most recent coffee. Further tests complement them:
integration tests of the stored rating, system tests of the status codes, and a Cucumber scenario of the first success
criterion. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "After recording a coffee, a user can rate the beans of that coffee: they choose the
  bean and give 1 to 5." "A user can rate their most recent own coffee until they record their next coffee, not only
  during the grace period. Rating again replaces their rating of that coffee. Each coffee has at most one rating." "If a
  coffee is undone or removed, its rating is removed too, and the coffee before it can be rated again."
- Depends on FR-13: FR-13 specifies how a user adds the coffee that a rating belongs to.
- Depends on FR-3 and FR-8: An undo (FR-3) and a correction to a lower coffee count (FR-8) remove coffees and their
  ratings and change which coffee is the most recent coffee.
- Depends on FR-6: A bean purchase creates the beans that a user can rate.
- Design, not part of this requirement: The table `coffee_ratings` (migration V9) refers to a user and a bean, but not
  to a coffee. The implementation has to store which coffee a rating belongs to, for example in a new migration.
- TBD (open): Can a user rate a coffee that a correction (FR-8) added? In the coffee app of SE@UHD, "a coffee that an
  admin recorded can't be rated". Contact person: group lead.
- The coffee app of SE@UHD has further functions for ratings, for which Coffee Kitty has no requirement yet: An admin
  changes or deletes a rating. A list shows the average score and the number of ratings of each bean. The ratings of a
  merged bean count for the bean that an admin merged it into.
