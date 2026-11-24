# FR-12: Rate a Bean

## Statement

When a user rates a bean with a score from 1 to 5, Coffee Kitty MUST store the score as the user's rating of their most
recent coffee.

## Rationale

The group buys different beans. A user rates the beans right after adding a coffee, and the ratings of a bean show the
group which beans to buy again.

## Success Criteria

- maxmustermann adds a coffee and rates the bean "Ethiopia Sidamo" with a score of 4. The bean has one rating, with a
  score of 4.
- maxmustermann then rates the same bean with a score of 2 before adding another coffee. The bean has one rating, with
  a score of 2.
- maxmustermann adds a second coffee and rates the bean with a score of 5. The bean has two ratings, with the scores 2
  and 5.
- With a coffee count of 0, a request to rate a bean returns 409 Conflict, and the bean has no rating.
- A request to rate a bean with a score of 0 or 6 returns 400 Bad Request, and the bean has no rating.
- A request to rate an unknown bean returns 404 Not Found.

## Verification Method

Test: unit tests of the limits for the score and of the rating of the most recent coffee, complemented by integration
tests of the stored rating, system tests of the status codes, and a Cucumber scenario of the first three success
criteria. All tests except the unit tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Source: the coffee app of SE@UHD, in which a user rates the bean of their most recent coffee until they add the next
  coffee, and a repeated rating replaces the earlier one (`CoffeeRatingService.rateCurrentBean`).
- Depends on the bean, the rating, and the score (GLOSSARY.md). The migration V9 creates the table `coffee_ratings`.
- The coffee app of SE@UHD has further rules for ratings, for which Coffee Kitty has no requirement yet: An undo removes
  the rating of the undone coffee. An admin corrects or deletes a rating. A list shows the average score and the number
  of ratings of each bean, and the ratings of a merged bean count for the bean that it was merged into.
