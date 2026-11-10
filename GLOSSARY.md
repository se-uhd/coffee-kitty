# Glossary

This glossary defines one term for each concept of Coffee Kitty. Use these terms in the documentation, in KDoc and
comments, and in the messages that the API returns. Identifiers such as class, field, table, and route names keep their
spelling, and the lines "API:" and "Code:" of an entry list the identifiers that differ from the term. Some entries end
with a line "Avoid:", which lists words not to use for the concept.

## Domain

- **user**: A person whose coffees the app counts. The app stores the login name, the email address, and the first and
  last name of the person.
  API: `/api/users`, `UserDto`, `userId`. Code: `User`, `UserService`, `users`.
  Avoid: member, drinker, account.
- **login name**: A user's unique name, which consists of ASCII letters, digits, and underscores and has at most 255
  characters. Two users can have names that differ only in case, such as `jane_doe` and `Jane_Doe`. `GET /api/users`
  sorts the users by this name, and the consumption endpoints return it together with the coffee count.
  API: `loginName`. Code: `User.loginName`, `login_name`.
  Avoid: login (for the name).
- **email address**: A user's unique address of the form name@domain, at most 254 characters long. Because the app
  trims the address and stores it in lowercase, `Jane@se.de` and `jane@se.de` count as the same address.
  API: `emailAddress`. Code: `User.emailAddress`, `email_address`.
  Avoid: email (for the address).
- **coffee**: One cup that a user drinks. Adding a cup raises the user's coffee count by one.
  API: `POST /api/users/{userId}/consumption` with `delta` 1. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: bump (a count).
- **coffee count**: A user's total number of coffees. The number starts at zero when the user is created and never
  decreases.
  API: `ConsumptionDto.count`. Code: `CoffeeConsumption.count`, `coffee_consumptions.count`.
- **consumption**: The stored record that holds the coffee count of one user. Every user has exactly one such record,
  which `UserService.create` creates together with the user.
  API: `/api/users/{userId}/consumption`, `ConsumptionDto`. Code: `CoffeeConsumption`, `coffee_consumptions`.
- **delta**: The field in the request body of `POST /api/users/{userId}/consumption` that states the change to the
  coffee count. The domain accepts only +1, which adds one coffee, and rejects every other integer. An extension can
  accept -1 to remove a coffee without changing the format of the request body (see the KDoc of
  `CoffeeConsumptionService.applyDelta`).
  API: `ConsumptionDeltaDto.delta`, `{ "delta": 1 }`. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: single-step change.
- **fixture user**: One of the five users in `TestFixtures`, such as `jane_doe`. In the dev profile, the fixture loader
  creates these users with a coffee count of zero on every start, and the system and acceptance tests create them the
  same way before each test.
  Code: `TestFixtures`, `FixtureStartupLoader`, `campus-coffee.fixtures.*`.
  Avoid: seeded user.

## Architecture

- **module**: One of the four Gradle subprojects that hold the app code: `domain`, `api`, `data`, and `application`.
  The architecture tests treat each of these subprojects as a layer. The fifth subproject, `detekt-rules`, holds build
  tooling instead of app code.
- **convention plugin**: A Gradle plugin in the included build `build-logic/` that configures one part of the build,
  such as the Java toolchain or the Kotlin compiler options with ktlint and detekt. A subproject applies such a plugin
  by the id `de.seuhd.campuscoffee.<name>`. Only the `data` module, for example, applies `kotlin-jpa-conventions`,
  which adds the Kotlin JPA plugin.
  Code: `java-conventions.gradle.kts`, `kotlin-conventions.gradle.kts`.
- **port**: An interface in `domain/.../ports/` that connects the domain to the other modules. The controllers call the
  API ports in `ports/api/`, and the data module implements the data ports in `ports/data/`. The internal ports in
  `ports/internal/` are for the fixture loader and the tests.
  Code: `UserService`, `UserDataService`, `DataResetService`.
- **adapter**: A class outside the domain that connects a port to a technology. Examples are a controller, which
  handles HTTP requests and calls an API port, and a data adapter such as `UserDataServiceImpl`, which implements a
  data port with Jakarta Persistence (JPA). The accepted ADR 001 applies the term to whole modules as well and calls
  the `api` module the REST adapter and the `data` module the JPA adapter.
  Code: `UserController`, `UserDataServiceImpl`.
- **entity**: A kind of domain object that the app stores in a table of its own, such as users and consumptions. Each
  kind needs several parts, such as a domain model, a data port, a JPA entity, an entity mapper, and a migration
  (CLAUDE.md, Adding a New Entity).
- **domain model**: A class in `domain/.../model/` that represents a concept of the business logic, such as a user, and
  has no annotations or code for the web or the database. An instance of such a class is a domain object.
  Code: `DomainModel`, `User`, `CoffeeConsumption`.
- **DTO**: A data transfer object, that is, a class in `api/.../dtos/` that defines the JSON body of a request or a
  response. Bean Validation checks a request body against the constraint annotations of the class, such as
  `@NotBlank`, before the controller method runs. A MapStruct mapper in `api/.../mapper/` converts between a domain
  object and the matching class. `UserDtoMapper` converts in both directions, and `ConsumptionDtoMapper` converts only
  from `CoffeeConsumption` to `ConsumptionDto`. `ConsumptionDeltaDto` has no mapper, because the controller passes the
  delta to the domain as a number.
  Code: `UserDto`, `ConsumptionDto`, `ConsumptionDeltaDto`, `UserDtoMapper`.
- **JPA entity**: A class in `data/.../persistence/entities/` that JPA maps to one table, such as `UserEntity` for the
  table `users`. Each such class extends the base class `Entity` and has a name that ends in `Entity`. An entity mapper
  converts between an instance of the class and the matching domain object.
  Code: `UserEntity`, `CoffeeConsumptionEntity`, `UserEntityMapper`.
- **upsert**: The operation of a data port that inserts or updates a domain object. A domain object without an id is
  inserted as a new row under a new random `UUID`. For a domain object with an id, the operation updates the stored row
  with that id, or throws a `NotFoundException` when no row has that id. A violation of a unique constraint that
  `ConstraintMapping.REGISTRY` lists becomes a `DuplicationException`.
  Code: `CrudDataService.upsert`, `CrudDataServiceImpl.upsert`.
- **migration**: A versioned SQL file in `data/src/main/resources/db/migration/`. At startup, Flyway applies every such
  file that it has not applied to the database before, in version order. Nobody edits a file that others have applied,
  for example a pushed file (CLAUDE.md, Database).
  Code: `V1__create_users_table.sql`, `V2__create_coffee_consumptions_table.sql`.
- **data reset**: The deletion of all consumptions and then all users through the internal port `DataResetService`.
  The fixture loader runs this deletion when `reset-on-startup` is true, and the data integration, system, and
  acceptance tests run it before each test. A client cannot delete the data through the API.
  Code: `DataResetService.clearAll`.
- **fixture loader**: The Spring component that creates the fixture users through `UserService.create` at startup when
  `campus-coffee.fixtures.load-on-startup` is true. With `campus-coffee.fixtures.reset-on-startup` set to true, as in
  the dev profile, the component runs a data reset first. Without that setting, the component creates the fixture users
  only when the database has no users.
  Code: `FixtureStartupLoader`, `FixturesProperties`.
  Avoid: startup loader.
- **dev profile**: The Spring profile `dev` for running the app locally. In this profile, the app serves the API and
  Swagger UI on port 8081 and connects to PostgreSQL on port 5433. At every start, the fixture loader runs a data reset
  and creates the fixture users. The tests run without this profile.
  Code: `application.yaml`, `--spring.profiles.active=dev`.
- **error response**: The response to a failed request. Its JSON body has the fields `errorCode`, `message`,
  `statusCode`, `statusMessage`, `timestamp` (in UTC), and `path`. For a domain exception, `errorCode` is the class name
  of the exception. For any other error, `errorCode` is the reason phrase of the status without spaces, such as
  `BadRequest`.
  Code: `ErrorResponse`, `GlobalExceptionHandler`.
- **domain exception**: An exception class in `domain/.../exceptions/`. `GlobalExceptionHandler` maps each of these
  classes to a status and uses the class name as the error code. `NotFoundException` (404) means that a lookup found no
  stored object, for example no user with the id in the path. `DuplicationException` (409) means that a request repeats
  a value that must be unique, such as the login name or the email address of another user. `ValidationException` (400)
  means that a request breaks a rule of the domain or of a controller, such as an integer delta other than +1 or an id
  in the body of `POST /api/users`. A request body that fails Bean Validation or cannot be read, such as a body with a
  delta of 1.9, gets a 400 with the error code `BadRequest` instead.
