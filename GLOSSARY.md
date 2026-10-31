# Glossary

This glossary gives one term for each concept of Coffee Kitty. Use the term in the documentation, the KDoc and
comments, and the messages that the API returns. Identifiers such as class, field, table, and route names keep their
spelling, so each entry adds the API and code names where they differ from the term. Some entries also list words to
avoid.

## Domain

- **user**: A person whose coffees the app counts, with a login name, an email address, and a first and a last name.
  API: `/api/users`, `UserDto`, `userId`. Code: `User`, `UserService`, `users`.
  Avoid: member, drinker, account.
- **login name**: A user's unique name of ASCII letters, digits, and underscores, at most 255 characters long. Login
  names that differ only in case are different login names. The list of users is ordered by login name, and a coffee
  count is shown with the login name of its user.
  API: `loginName`. Code: `User.loginName`, `login_name`.
  Avoid: login (for the name).
- **email address**: A user's unique address of the form name@domain, at most 254 characters long. The app stores it
  trimmed and in lowercase, so `Jane@se.de` and `jane@se.de` are the same email address.
  API: `emailAddress`. Code: `User.emailAddress`, `email_address`.
  Avoid: email (for the address).
- **coffee**: One cup that a user drinks. Adding a coffee raises the user's coffee count by one.
  API: `POST /api/users/{userId}/consumption` with `delta` 1. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: bump (a count).
- **coffee count**: A user's running total of coffees, which starts at zero and only grows.
  API: `ConsumptionDto.count`. Code: `CoffeeConsumption.count`, `coffee_consumptions.count`.
- **consumption**: The record that holds one user's coffee count. Every user has exactly one consumption, created
  together with the user.
  API: `/api/users/{userId}/consumption`, `ConsumptionDto`. Code: `CoffeeConsumption`, `coffee_consumptions`.
- **delta**: The field of the request body that states the change to a coffee count. The domain accepts only +1,
  which adds one coffee. An extension can accept a delta of -1 to remove a coffee without changing the request
  body (see the KDoc of `CoffeeConsumptionService.applyDelta`).
  API: `ConsumptionDeltaDto.delta`, `{ "delta": 1 }`. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: single-step change.
- **fixture user**: One of the five users that the dev profile seeds on every start and the system and acceptance
  tests seed before each test, each with a coffee count of zero.
  Code: `TestFixtures`, `FixtureStartupLoader`, `campus-coffee.fixtures.*`.
  Avoid: seeded user.

## Architecture

- **module**: One of the Gradle subprojects `domain`, `api`, `data`, and `application`, each a layer in the
  architecture tests. The `detekt-rules` subproject holds build tooling and is not a module.
- **convention plugin**: A Gradle plugin in the included build `build-logic/` that holds one part of the build setup,
  such as the Java toolchain, the Kotlin options with ktlint and detekt, or the Kotlin JPA plugin that only `data`
  needs.
  A subproject applies it by the id `de.seuhd.campuscoffee.<name>`.
  Code: `java-conventions.gradle.kts`, `kotlin-conventions.gradle.kts`.
- **port**: An interface in `domain/.../ports/` that connects the domain to the rest of the app. The controllers call
  the API ports in `ports/api/`, and the data module implements the data ports in `ports/data/`. The fixture loader
  and the tests also use the internal ports in `ports/internal/`.
  Code: `UserService`, `UserDataService`, `DataResetService`.
- **adapter**: A class outside the domain that connects a port to a technology, such as a controller that calls an API
  port or a data adapter, which implements a data port with Jakarta Persistence (JPA). The accepted ADR 001 also calls
  the `api` module the REST adapter and the `data` module the JPA adapter, after the adapters that they hold.
  Code: `UserController`, `UserDataServiceImpl`.
- **entity**: A kind of domain object that the app stores in its own table, such as a user or a consumption. Each
  entity has a domain model, a data port, a JPA entity, an entity mapper, and a migration (CLAUDE.md, Adding a New
  Entity).
- **domain model**: A class in `domain/.../model/` that represents a concept of the business logic, such as a user,
  without any web or database detail. An instance is a domain object.
  Code: `DomainModel`, `User`, `CoffeeConsumption`.
- **DTO**: A data transfer object, a class in `api/.../dtos/` that defines the JSON body of a request or a response.
  Bean Validation checks a request DTO, and a DTO mapper converts a domain object to its DTO. `UserDtoMapper` also
  converts back, `ConsumptionDtoMapper` does not, and `ConsumptionDeltaDto` has no mapper, because the controller
  passes its delta to the domain as a number.
  Code: `UserDto`, `ConsumptionDto`, `ConsumptionDeltaDto`, `UserDtoMapper`.
- **JPA entity**: A class in `data/.../persistence/entities/` that JPA maps to one table, named `*Entity` and extending
  `Entity`. An entity mapper converts between a JPA entity and its domain object.
  Code: `UserEntity`, `CoffeeConsumptionEntity`, `UserEntityMapper`.
- **upsert**: Storing a domain object through a data port. An object without an id is inserted under a new random
  `UUID`. For an object with an id, `upsert` updates the stored row, or throws a `NotFoundException` when no row has
  that id. A violated unique constraint that `ConstraintMapping.REGISTRY` lists becomes a `DuplicationException`.
  Code: `CrudDataService.upsert`, `CrudDataServiceImpl.upsert`.
- **migration**: A versioned SQL file in `data/src/main/resources/db/migration/` that Flyway applies once, in version
  order, at startup. A migration that others have applied, such as a pushed one, is never edited (CLAUDE.md,
  Database).
  Code: `V1__create_users_table.sql`, `V2__create_coffee_consumptions_table.sql`.
- **data reset**: Deleting every consumption and then every user through the internal port `DataResetService`. The
  fixture loader runs it under `reset-on-startup`, and the tests run it before each test. No endpoint offers it.
  Code: `DataResetService.clearAll`.
- **fixture loader**: The component that creates the fixture users through `UserService.create` on startup when
  `campus-coffee.fixtures.load-on-startup` is true. When `campus-coffee.fixtures.reset-on-startup` is also true, as in
  the dev profile, it first runs a data reset. Otherwise, it creates the fixture users only in a database without
  users.
  Code: `FixtureStartupLoader`, `FixturesProperties`.
  Avoid: startup loader.
- **dev profile**: The Spring profile `dev` for a local run. It serves the API on port 8081 with Swagger UI, connects
  to PostgreSQL on port 5433, and starts every run with a data reset and the fixture users. The tests run without it.
  Code: `application.yaml`, `--spring.profiles.active=dev`.
- **error response**: The response to a failed request, with a JSON body that has the fields `errorCode`, `message`,
  `statusCode`, `statusMessage`, `timestamp` (in UTC), and `path`. Its error code is the class name of a domain
  exception or, for any other error, the reason phrase of the status without spaces, such as `BadRequest`.
  Code: `ErrorResponse`, `GlobalExceptionHandler`.
- **domain exception**: One of the exceptions in `domain/.../exceptions/`, which the exception handler maps to a
  status: `NotFoundException` (404) for an unknown id or user, `DuplicationException` (409) for a value that violates a
  unique constraint, such as a taken login name or email address, and `ValidationException` (400) for a request that
  breaks a rule of the domain or the controller, such as an integer delta other than +1 or an id in the body of
  `POST /api/users`. A request body that fails Bean Validation gets a 400 with the error code `BadRequest` instead, as
  does a body that cannot be read, such as one with a delta of 1.9.
