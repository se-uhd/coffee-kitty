# How do the JPA entities map their ids and their references?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

Since the first version of 2026-10-31, `CrudDataServiceImpl.upsert` assigns a random `UUID` to a new row, and the
migrations declare the column `id` without a default. Spring Data treats an entity with an id as stored, so `save()`
of such an entity calls `merge`, which runs a SELECT for the row before the INSERT. The tables of the later features of
2026-11-24 add two cases. A merged bean refers with `merged_into_id` to the bean into which an admin merged it, and
that bean can be merged in turn. The tables `user_balance` and `kitty_balance` have neither a random id nor
timestamps: The id of a balance is the id of its user, and the kitty balance has the only id 1. The KDoc of the JPA
entities gave the reasons below. The cleanup of the comments on 2026-11-26 (ADR 013) shortened them and removed the
note that `save()` of a new balance runs a SELECT.

## Considered Options

* The defaults of Spring Data and JPA: Each JPA entity declares its own `@Id`, `save()` decides by the id whether a row
  is new, and each foreign key is an association. Every insert then runs a SELECT first.
* Ids that PostgreSQL generates (`@GeneratedValue`), so that `save()` detects a new entity by its null id. Each table
  then needs a default for its id, such as `gen_random_uuid()`, which no migration declares.
* A base class that implements `Persistable` for the entities with a random id, a plain id column for
  `merged_into_id`, and no base class for the two balance tables.

## Decision(s)

The JPA entities follow the third option:

* `PersistableEntity` implements `Persistable`, and `isNew` returns true until JPA loads or inserts the row, so `save()`
  inserts a new entity without the SELECT. `Entity` extends it with the timestamps, and every other JPA entity extends
  `Entity`.
* `CoffeeBeanEntity.mergedIntoId` is a `UUID` column and not a `@ManyToOne` association, because JPA would load the
  chain of merged beans through an association, which is eager by default (ADR 004).
* `UserBalanceEntity` and `KittyBalanceEntity` extend neither `Entity` nor `PersistableEntity`, because their tables
  have neither a random id nor timestamps.

## Consequences

* `EntityMapper` and `CrudDataServiceImpl` accept only subclasses of `Entity`, so a data adapter for a balance reads
  and writes its table through the repository (`.claude/rules/later-features.md`).
* The balance entities do not implement `Persistable`, so `save()` of a new balance runs a SELECT for the row before the
  INSERT.
