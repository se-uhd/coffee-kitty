# Which delete action and which index does each foreign key have?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The migrations declare eleven foreign keys: V2 of 2026-10-31, V3 of 2026-11-17, and V5 and V7 to V11 of 2026-11-24. A
foreign key without an `ON DELETE` clause has the default action `NO ACTION`, under which PostgreSQL refuses to delete a
row while another row refers to it. PostgreSQL creates an index for a primary key and for a unique constraint, but not
for the referencing column of a foreign key. Without such an index, PostgreSQL scans the whole table for a query by
that column and for the referencing rows when it deletes a referenced row. The KDoc of the JPA entities gave the
reasons of most actions and of the indexes of V5, V7, and V9. The cleanup of the comments on 2026-11-26 (ADR 013)
removed the reasons of the indexes and kept the reasons of the actions in shortened KDoc. `.claude/rules/data.md`
states the rule for a new foreign key.

## Considered Options

* `ON DELETE CASCADE` on every foreign key. Deleting a user then also deletes the deposits and the expenses of the
  user, although they moved money, and the corrections that the user made as an admin.
* The default `NO ACTION` on every foreign key. A user can then be deleted only after the consumption, the coffee
  times, the corrections, the ratings, and the balance of the user, which exist only for that user.
* `CASCADE` for a row that belongs to the referenced row, `NO ACTION` for every other foreign key, and an index that
  starts with the column of each foreign key.

## Decision(s)

Each foreign key follows the third option. Every index that starts with the column of a foreign key also serves the
lookup of the referencing rows when PostgreSQL deletes a referenced row.

* `coffee_consumptions.user_id`: `CASCADE`, because a consumption belongs to its user. The unique constraint
  `uq_coffee_consumptions_user` provides the index.
* `coffee_times.consumption_id`: `CASCADE`, because a coffee time belongs to its consumption. The primary key
  `(consumption_id, position)` provides the index.
* `count_corrections.user_id`: `CASCADE`, because a correction belongs to its user, as a consumption does. The index on
  `(user_id, created_at)` serves `getAllByUserId`, which returns the corrections of a user, newest first.
* `count_corrections.admin_id`: `NO ACTION`, because a correction does not belong to its admin, so PostgreSQL does not
  delete an admin who corrected the coffee count of another user. The index on `admin_id` serves the deletion of a user.
* `payments.user_id`: `NO ACTION`, because a deposit is money in the kitty and must not disappear with its user. The
  index on `(user_id, created_at)` serves the query of the deposits of a user, newest first.
* `coffee_beans.merged_into_id`: `NO ACTION`, so PostgreSQL does not delete a bean while a merged bean refers to it.
* `coffee_ratings.user_id`: `CASCADE`, because a rating belongs to its user. The index on `(user_id, created_at)`
  serves the query of the ratings of a user, newest first.
* `coffee_ratings.bean_id`: `NO ACTION`, so PostgreSQL does not delete a bean with a rating. The index on `bean_id`
  serves the query of the ratings of a bean.
* `expenses.buyer_user_id` and `expenses.bean_id`: `NO ACTION`, because an expense moved money and must not disappear
  with its buyer or its bean. The index on `(buyer_user_id, created_at)` serves the query of the expenses of a buyer,
  newest first.
* `user_balance.user_id`: `CASCADE`, because the row holds the balance of one user, as a consumption holds the coffee
  count. The primary key provides the index.

## Consequences

* Deleting a user deletes the consumption, the coffee times, the corrections, the ratings, and the balance of the user.
  PostgreSQL refuses to delete a user with a deposit or an expense or who corrected another coffee count as an admin,
  and a bean with a rating, an expense, or a merged bean. The data port then throws a `DataConstraintException`, which
  becomes a 500, so an operation that deletes such a row checks the references first (`.claude/rules/data.md`).
* No endpoint deletes a user or a bean yet. The data integration tests check the action of each foreign key, such as
  `deleting an admin who corrected another coffee count throws DataConstraintException`.
* A new foreign key gets its action from this rule and an index that starts with its column in the same migration.
