---
description: "the migrations, the JPA entities, the data adapters, and the entity mappers."
paths:
  - "data/**"
---

# Data module

## Migrations

- Flyway applies the migrations in `data/src/main/resources/db/migration/`. Write them in plain DDL without comments.
- Never edit a migration that others have applied, such as a pushed or released one. Add a new migration instead.
  Flyway compares the checksum of every applied migration at startup, so any change to an applied file stops the app.
- You may edit a migration that Flyway has applied only to your own dev database. Then remove that database with
  `docker rm -f -v campus-coffee-db` and start a new container with the `docker run` command of `CLAUDE.md`.
- Once the app runs in a rolling deployment, keep each migration compatible with the previous release, because its
  instances serve requests while the next release migrates the schema. Then a renamed or dropped column, or a new NOT
  NULL column without a default, ships in two releases. The first adds and backfills the new column, and the second
  removes the old column or adds the NOT NULL constraint.
- At startup, Hibernate checks that every mapped table and column exists with a compatible type (`ddl-auto: validate`)
  and never changes the schema. The check ignores the NOT NULL, CHECK, and unique constraints and every unmapped
  column, so a violation of these constraints fails only when the statement runs. Test each constraint in an
  integration test.
- Choose the `ON DELETE` action of each foreign key. Use `CASCADE` only for a row that belongs to the referenced row,
  such as a consumption, which belongs to its user. Under the default `NO ACTION`, deleting a referenced row throws a
  `DataConstraintException`, and the client gets a 500. An operation that deletes a row must therefore check for
  references first and, for a referenced row, throw a new domain exception that `GlobalExceptionHandler` maps to 409
  Conflict.

## JPA entities and data adapters

- A JPA entity extends `Entity`. Write its annotation fully qualified as `@jakarta.persistence.Entity`, because an
  import of `jakarta.persistence.Entity` hides the base class `Entity`.
- Add a `ConstraintMapping.REGISTRY` entry for each unique constraint. Without the entry, a violated unique constraint
  becomes a 500 instead of a `DuplicationException` (409).
- A data adapter extends `CrudDataServiceImpl`. No exception of Spring or Hibernate crosses a data port.
- The read functions of the data adapters do not run in a transaction (ADR-004), and `application.yaml` turns off Open
  Session in View. A data adapter for a JPA entity with a lazy association must read and map the entity inside a
  `@Transactional` function.

## Entity mappers

- An entity mapper extends `EntityMapper` with the domain type first, as in
  `UserEntityMapper : EntityMapper<User, UserEntity>`. It overrides `updateEntity` to ignore the id and the timestamps
  (see `UserEntityMapper`). Without the override, an update copies them from the domain object.
- Map each reference to another entity by its id (see `CoffeeConsumptionEntityMapper`). Add a lookup such as
  `userReference` to `EntityReferenceResolver`, list the resolver and the referenced mapper in `@Mapper(uses = ...)`,
  and put `@Mapping(target = "<reference>", source = "<reference>.id")` on `toEntity` and `updateEntity`. Without these
  mappings, MapStruct maps the nested domain object through the referenced mapper, and an update writes its fields over
  the referenced row.
