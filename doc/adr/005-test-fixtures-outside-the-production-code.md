# Where are the test fixtures and the code that deletes data?

## Decision Metadata

### Status

Accepted

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The tests of every module share test objects, such as the fixture users, and every test needs an empty database. In
the dev profile, the app also created the fixture users on startup. Until this decision, `TestFixtures` was in the main
sources of the domain module, because the fixture loader was production code. The data reset, which deleted the data of
every data port, was production code as well, and the property `campus-coffee.fixtures.reset-on-startup` enabled it. A
deployed app with this property would have deleted its data. The coverage gate, PIT, and the architecture tests each
needed a special case for the fixture package.

## Considered Options

* Keep the fixtures and the data reset in the production code and allow the reset only in the dev profile. The code
  that deletes data is still part of every deployed app.
* Keep the five fixture users in the production code of the application module for the dev profile, and move the other
  test objects to the test sources. Two places then define users for tests.
* Move every fixture and the code that deletes data to the Gradle test fixtures (`java-test-fixtures`), and start the
  app with fixtures from a `main` function in the test sources with `bootTestRun`, which Spring Boot provides for
  development-time setup.

## Decision(s)

The fixtures and the code that deletes data are in the test fixtures (the third option). `TestFixtures` is in
`domain/src/testFixtures`, and `DatabaseCleaner` in `data/src/testFixtures` replaces the data reset. The dev application
in `application/src/test/.../dev/` cleans the database and creates the fixture users on every start. A deployed app
cannot delete its data, because the code is not on its classpath.

## Consequences

* `gradle :application:bootTestRun` starts the app with the fixture users. `bootRun` with the dev profile starts it on
  the data of the previous run.
* `DataResetService`, `CrudDataService.clear`, `FixtureStartupLoader`, `FixturesProperties`, and the keys under
  `campus-coffee.fixtures` are removed.
* `DatabaseCleaner` truncates every table that the PostgreSQL catalog lists. A new table needs no change to it, and the
  order of the foreign keys does not matter. The statement is specific to PostgreSQL.
* The coverage gate, PIT, and the architecture tests have no special case for fixtures. An architecture test fails if
  the test fixtures are imported as production classes.
* The dev application is test code. detekt does not check it, and the coverage gate does not count it.
