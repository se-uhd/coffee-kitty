# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- An admin can correct the coffee count of a user at any time and has to add a note with the reason for the correction
  (FR-8 in `requirements/FR-8-correct-coffee-count.md`). `PUT /api/users/{userId}/consumption` with
  `{ "total", "note" }` sets the coffee count and returns 403 Forbidden when the acting user is not an admin. It returns
  400 Bad Request for a negative total, a total above 1,000,000, or a note that is missing, blank, or longer than 500
  characters. A lower count removes the newest coffees, and an undo then applies to the newest coffee that remains. The
  migration V5 creates the table `count_corrections`, which stores each correction with the count before and after, the
  note, and the admin. `GET /api/users/{userId}/consumption/corrections` lists the corrections of a user, newest first,
  and returns 403 Forbidden when the acting user is not an admin.
- The database has the tables of later features, which no endpoint uses yet. The migration V6 creates the table
  `coffee_prices` (the price per cup, at most one row). The migrations V7 to V9 create the tables `payments` (deposits
  and kitty adjustments), `coffee_beans` (beans with a unique name among the live beans), and `coffee_ratings` (scores
  from 1 to 5). The domain module has the new domain models `CoffeePrice`, `Payment`, `CoffeeBean`, and `CoffeeRating`.
  The data module implements the new data ports `CoffeePriceDataService`, `PaymentDataService`, `CoffeeBeanDataService`,
  and `CoffeeRatingDataService`, which store them. The migrations V10 and V11 create the tables `expenses`,
  `user_balance`, and `kitty_balance`, which have only a JPA entity and a repository so far. The section "Tables Without
  an Endpoint" in `CLAUDE.md` lists what each feature still needs, and GLOSSARY.md defines the new terms.
- The requirements FR-4 (record a deposit), FR-10 (raise the kitty balance), and FR-6 (record own expenses) in
  `requirements/`. Coffee Kitty does not implement them yet. FR-10 had the ID FR-4b until now. A requirement ID has no
  suffix, and each requirement lists the requirements that it depends on and those that depend on it.
- The requirements FR-5 (pay for a coffee) and FR-11 (credit an undone coffee) in `requirements/`. FR-11 specifies that
  an undo raises the balance by the amount by which the coffee lowered it, even when the price per cup changed in
  between. The migration V12 adds the column `coffee_times.price_cents` for this amount. Until now, the table stored
  only the time of a coffee. Coffee Kitty does not implement FR-5 and FR-11 yet, and no JPA entity maps the column.
- The requirement FR-12 (rate a bean) in `requirements/`, which follows the coffee app of SE@UHD. Coffee Kitty does not
  implement it yet.
- A client can read the undo status of a user (FR-9 in `requirements/FR-9-undo-status.md`).
  `GET /api/users/{userId}/consumption/undo` returns `undoable` and `undoableUntil`, the end of the grace period of the
  most recent coffee or null. `undoable` is true exactly when `POST` to the same path would undo a coffee at that time.
  `GracePeriod.closesAt` returns the end of a grace period, and `ConsumptionRequests.retrieveUndoStatus` sends the
  request for the system tests. GLOSSARY.md defines the term undo status.
- `TestFixtures` has test fixtures beyond the user fixtures. `admin()` and `regularUser()` return one user fixture each,
  and `FIXED_TIME` and `fixedClock()` set the time of a test. `consumptionWithoutCoffee`, `consumptionWithRecentCoffee`,
  and `consumptionWithOldCoffee` return consumptions in three states. GLOSSARY.md defines the term test fixture.
- Unit tests of the domain: `ConsumptionPropertiesTest`, `IdentifiableTest`, and `DomainExceptionsTest`.
  `GracePeriodTest`, `CoffeeConsumptionServiceTest`, and `UserServiceTest` have further tests, such as an undo of the
  only coffee and a login name of one character.
- `StartupTest` checks that the app creates no user on startup without a configured bootstrap admin.
- Integration tests: `CoffeeConsumptionDataServiceIntegrationTest` checks that the database stores and deletes the
  times of the coffees and rejects a negative coffee count. `CrudDataServiceIntegrationTest` checks that
  `UserDataServiceImpl` returns the users ordered by login name and stores the role of a user.
- System tests for an undo at a coffee count of zero and for an undo with a grace period of zero
  (`UndoWithoutGracePeriodSystemTests`). Further system tests check login names of 1, 255, and 256 characters, an email
  address with capital letters, and an admin whom another admin created. They also check the URL in the `Location`
  header of a new user and a text as the delta. `FailureSystemTests` checks that a request returns 500 and stores
  nothing when a data adapter throws an exception.
- The undo status (FR-9) has no tests yet, because they are the reference solution of assignment 7. Until 2026-12-08,
  the coverage gate and PIT leave out the functions `undoableUntil`, `GracePeriod.closesAt`, and
  `ConsumptionController.undoStatus`.
- `gradle build` creates a coverage report of all modules with JaCoCo. The build fails below 95% of the lines or 85% of
  the branches, and when the tests do not execute every function of a domain service or a controller.
- `gradle :domain:pitest` runs mutation tests of the domain services with PIT and fails when the tests detect fewer
  than 90% of all mutants. `gradle build` does not run it, and the CI workflow runs it after the build.
- The system and acceptance tests check the JSON field names of the API. They send every request body as a map with the
  field names and parse the responses into the classes of `Json.kt` with a mapper that fails on an unknown or a missing
  field. Until now, they used the DTO classes of the api module and did not detect a renamed field.
- Architecture tests for four more rules: The data module uses only the models, the exceptions, and the data ports of
  the domain. A controller function that reads the acting user has the annotation `ActingUserRequired`. The system and
  acceptance tests use no class of the api module. The production classes contain no test fixtures.
- ADRs 002 to 004 in `doc/adr/` record the decisions on Spring in the domain, the role checks, and the read methods
  without a transaction. ADRs 005 to 007 record the decisions on the test fixtures, the validation, and the tests of the
  JSON field names.
- `gradle build` runs CPD, the copy-paste detector of PMD, on the production code and fails at a duplicate of 70 tokens.

### Changed

- Breaking: The api module checks only the structure of a request body, and the domain checks every value. A 400 has the
  error code `BadRequest` for a missing field, a value of the wrong JSON type, or an unknown field, and the error code
  `ValidationException` for a value that breaks a rule. The error code therefore changes from `BadRequest` to
  `ValidationException` for a login name that is too long or has an invalid character and for a blank first or last
  name. The same change applies to a total of a correction that is negative or above 1,000,000 and to a note that is
  blank or longer than 500 characters. Until now, both modules checked these rules, and the email address had to pass
  two different checks.
- Breaking: A request body with an unknown field returns 400 with the error code `BadRequest`
  (`spring.jackson.deserialization.fail-on-unknown-properties`). `POST /api/users` with an `id` returned 400 with the
  error code `ValidationException` until now, and the server ignored the fields `createdAt` and `updatedAt` and any
  misspelled field.
- `POST /api/users` reads the new `CreateUserDto`, and `UserDto` is only the response body. The OpenAPI definition marks
  every field of a response body as required. Until now, it marked none. `Dto` and `DtoMapper` no longer exist.
- The production jar contains neither the user fixtures nor code that deletes data. `TestFixtures` is in the test
  fixtures of the domain module (`src/testFixtures`), and the new `DatabaseCleaner` in the test fixtures of the data
  module replaces `DataResetService`. `DatabaseCleaner` truncates every table that the PostgreSQL catalog lists,
  including `expenses`, `user_balance`, and `kitty_balance`, which the data reset left out. `CrudDataService.clear`,
  `FixtureStartupLoader`, `FixturesProperties`, and the keys `campus-coffee.fixtures.load-on-startup` and
  `campus-coffee.fixtures.reset-on-startup` no longer exist. `DatabaseCleaner` is a Spring bean, which the tests
  inject, so that only the data module uses the `DataSource`.
- `gradle :application:bootTestRun` starts the app with the user fixtures (the dev application). Until now,
  `gradle :application:bootRun --args='--spring.profiles.active=dev'` started the app with the user fixtures. This
  command now starts the app on the data of the previous run.
- A data adapter throws the new `DataConstraintException` of the domain module for a violated CHECK constraint and for
  a deletion that a foreign key prevents, instead of Spring's `DataIntegrityViolationException`. The client still gets
  a 500.
- `CountCorrectionDataService` has only `insert` and `getAllByUserId`, because a stored correction never changes.
- `SecurityConfig` reads the routes that need an acting user from the controller functions with the new annotation
  `ActingUserRequired` instead of a list of paths.
- The unit tests of the api module are in `api/src/test`.
- The messages of three errors have new wording: "The coffee count must not be negative.", "The coffee count must not
  exceed 1000000.", and "The grace period of the most recent coffee has passed." The message for an invalid first or
  last name states the rules in parallel form. In the OpenAPI definition, the description of the path parameter
  `userId` is "The id of the user.", and the summary of `GET /users/{userId}/consumption/undo` uses the term undo
  status.
- The data adapters are in the package `data.implementation`, as the domain services are in `domain.implementation`.
  Until now, the package was `data.implementations`.
- GLOSSARY.md defines the term endpoint, and the documentation no longer uses the word route for it. The message of a
  missing object writes "id" in lowercase ("User with id ... does not exist.").
- GLOSSARY.md calls a user of `TestFixtures` a user fixture instead of a fixture user, and the code and the
  documentation use this term.
- The tests check observable behavior, such as a status, an error code, an exception type, or the stored state. They no
  longer check implementation details, such as the wording of a message, an annotation, or a call between domain
  classes. Tests that only repeated another test no longer exist.
- The tests run in the time zone Asia/Tokyo, so that a test fails when the production code uses the time zone of the
  JVM for a time in UTC.
- The domain exceptions extend the sealed class `DomainException`, and `GlobalExceptionHandler` no longer has code for
  cases that cannot occur.

### Fixed

- Every error message is in English. Until now, `POST /api/users` with `Accept-Language: de` returned a message that
  mixed German and English, because Spring took the locale from this header and Hibernate Validator translates its
  default messages. `application.yaml` now sets the locale to English for every request.
- The message of a 401 for a blank `X-Acting-User` header is "The header X-Acting-User is missing or blank." Until now,
  the message was "The header X-Acting-User is missing."

## [0.2.3] - 2026-11-22

### Fixed

- A request with a wildcard in its `Content-Type`, such as `*/*` or `application/*`, gets a 415 with the error code
  `UnsupportedMediaType`. Until now it got a 500, because Spring rejects such a header with an
  `IllegalArgumentException` while it reads the request, and `GlobalExceptionHandler` mapped that exception like a
  failed `require`.

## [0.2.2] - 2026-11-22

### Fixed

- `POST /api/users/{userId}/consumption` rejects a delta that is a JSON string, such as `{"delta": "1"}`, with a 400
  and the error code `BadRequest`. Until now Jackson read the string as the number 1, and the request added a coffee.
  `application.yaml` sets `spring.jackson.mapper.allow-coercion-of-scalars` to false, so the same holds for every
  number and boolean field of a request body.
- `POST /api/users` rejects a JSON number for the role, such as `"role": 1`, and a JSON number or boolean for a `String`
  field, such as `"loginName": 12345`, with a 400 and the error code `BadRequest`. Until now Jackson read the number 1
  as the role `ADMIN`, which is the constant with the index 1. It stored a number or a boolean as its text, such as the
  login name "12345". `application.yaml` sets `spring.jackson.datatype.enum.fail-on-numbers-for-enums` to true for every
  enum field of a request body. The new `JsonConfig` rejects a number or a boolean for every `String` field, because
  Spring Boot has no property for this setting.
- The REST API reads and writes only JSON, as the README states. A request with a YAML body, such as `POST /api/users`
  with `Content-Type: application/yaml`, gets a 415 with the error code `UnsupportedMediaType`, and a request with
  `Accept: application/yaml` gets a 406. Until now such a body got a 500, and such an `Accept` header got a YAML
  response. The reason was that springdoc puts the YAML module of Jackson on the classpath, and Spring registered a YAML
  message converter for it. `ApiWebConfig` removes this message converter as it removes the XML message converters.

## [0.2.1] - 2026-11-17

### Added

- GLOSSARY.md defines the terms correction and note of FR-8.
- `SecurityConfig` requires an acting user for the requests of FR-8, `PUT /api/users/{userId}/consumption` and
  `GET /api/users/{userId}/consumption/corrections`, so that an implementation of FR-8 does not have to change the
  security configuration. Until Coffee Kitty implements FR-8, a request with an acting user gets 405 or 404.

### Changed

- The step definitions of FR-3 in `CucumberConsumptionSteps` send their requests through `ConsumptionRequests`
  instead of building HTTP requests, and their Then steps check the stored coffee count.
- The system tests send their requests through the new `UserRequests` and through new functions of
  `ConsumptionRequests` instead of building HTTP requests. These functions return a `Response` with the status code,
  the headers, and the body or the error response. Only the tests of an unknown path, of a path that the firewall
  rejects, and of the OpenAPI definition build their own requests.

### Fixed

- FR-8 identifies the TBD of FR-3 on corrections by the question of the TBD instead of by a wrong position and contains
  the decision of the group lead as its business requirement. The Rationale of FR-8 no longer specifies who reads the
  notes, which a TBD of FR-8 leaves open.

## [0.2.0] - 2026-11-17

### Added

- A user can undo their most recent coffee within 5 minutes of adding it (FR-3 in `requirements/FR-3-undo-coffee.md`).
  `POST /api/users/{userId}/consumption/undo` lowers the coffee count by one and returns 409 Conflict when the user has
  no coffee to undo or when the grace period of the most recent coffee has ended. After an undo, the user can undo the
  coffee before while its own grace period is open. The grace period includes its last instant, as in the coffee app of
  SE@UHD, and `campus-coffee.consumption.grace-period` sets it (default 5 minutes). The migration V3 creates the table
  `coffee_times`, which holds the time of each recent coffee that the user did not undo. Adding a coffee drops the times
  of the coffees outside their grace period. Coffees added before this release have no time, and a user cannot undo
  them.
- Every user has a role, `USER` or `ADMIN` (migration V4), and the fixture user `jane_doe` is an admin. `POST
  /api/users` may set the role of the new user, which defaults to `USER`.
- Spring Security runs `ActingUserAuthenticationFilter` before the controllers. The filter resolves the acting user
  from the header `X-Acting-User`, and `SecurityConfig` lists the routes that need one. A request without a known acting
  user gets 401 Unauthorized before Spring MVC reads its body, and every 401 has the header `WWW-Authenticate` with the
  challenge `X-Acting-User realm="Coffee Kitty"`, as RFC 9110 requires.
- FR-8 in `requirements/FR-8-correct-coffee-count.md` specifies the correction of a coffee count by an admin, which
  the group lead decided and which assignment 5 implements. FR-3 marks its open question about corrections as
  resolved.
- `ConsumptionRequests` in the tests sends the requests of the step definitions, so that a step names users by login
  name and gets status codes and values back. Examples of these requests are adding a coffee and reading a coffee count.
  Its functions for FR-8 send the correction and read the corrections, which Coffee Kitty does not implement
  before assignment 5.
- When no admin exists, `BootstrapAdminLoader` stores an admin on startup, after the fixture loader, from the keys under
  `campus-coffee.bootstrap-admin`. The environment variables `BOOTSTRAP_ADMIN_LOGIN_NAME`,
  `BOOTSTRAP_ADMIN_EMAIL_ADDRESS`, `BOOTSTRAP_ADMIN_FIRST_NAME`, and `BOOTSTRAP_ADMIN_LAST_NAME` set these keys. If a
  user has the configured login name, the loader gives that user the role `ADMIN`.

### Changed

- Breaking: Only an admin may create a user with `POST /api/users`. Coffee Kitty has no authentication yet. A client
  sends the login name of its acting user in the header `X-Acting-User`. The response is 401 Unauthorized without a
  known acting user and 403 Forbidden when the acting user is not an admin. The migration V4 gives every existing user
  the role `USER`, including `jane_doe` in a database that the `dev` profile does not reset. Configure the bootstrap
  admin with the login name of an existing user, or of a new one, before creating users.
- The default firewall of Spring Security rejects a path with `;`, `//`, `..`, or an encoded slash and a header value
  with a control character. The firewall rejects such a request because security rules and Spring MVC can read it
  differently. A rejected request gets 400 Bad Request with the message "The request could not be read." Coffee Kitty
  0.1.0 handled `GET /api/users;x=1` like `GET /api/users` and responded to `GET /api//users` with 404 Not Found.
- The README describes how the group buys coffee beans from the kitty and how a user who buys coffee with their own
  money declares the purchase as an expense. It states that students first see the app in the lectures on agentic
  software engineering and explains how to set up IntelliJ IDEA.
- The README no longer tells readers to clone the repository, because they have already cloned it when they open the
  README in IntelliJ IDEA. The setup instructions start with `mise trust` and continue with `mise install`, and every
  command in the README has its own code block.
- `gradle check` runs ktlint on the root `build.gradle.kts` and `settings.gradle.kts` and on the included build
  `build-logic`, in addition to the subprojects. ktlint found four formatting violations in
  `java-conventions.gradle.kts`, which this release fixes.
- Coffee Kitty uses Gradle 9.9 instead of 9.8 (`mise.toml`), and `mise install` installs it. In IntelliJ IDEA, select
  the `gradle-9.9.x` directory as the local Gradle installation (README, section IntelliJ IDEA).
- For a failed test, Gradle prints the assertion message, e.g., `expected: 200 but was: 404`, and the stack trace with
  the class and method of each line, which IntelliJ IDEA links to the source.
- The workflow `.github/workflows/mise-outdated.yml` checks the pins in `mise.toml` every Monday, because Dependabot
  does not read `mise.toml`. The workflow opens an issue when a pin no longer covers the newest release of its tool.
- When IntelliJ IDEA runs a single scenario of a feature file, Cucumber loads the step definitions from the package of
  `CucumberTests` (`application/src/test/resources/cucumber.properties`) and no longer scans every test class.

### Fixed

- A successful build no longer ends with the deprecation notice that detekt 2.0.0-alpha.6 causes. Until a detekt
  release includes the fix from detekt/detekt#9577, `org.gradle.warning.mode=none` in `gradle.properties` hides all
  Gradle deprecation warnings.
- IntelliJ IDEA could not start the Gradle daemon when it ran Gradle on a JDK older than 23, because such a JDK rejects
  the option `--sun-misc-unsafe-memory-access=allow` in `org.gradle.jvmargs`. `gradle/gradle-daemon-jvm.properties` now
  requires JDK 25 for the daemon. When Gradle finds no JDK 25 on the machine, it downloads a JDK 25 from the URLs in
  this file. The foojay toolchain resolver in `settings.gradle.kts` looked up these URLs.

## [0.1.0] - 2026-10-31

### Added

- A REST API that lists and creates users, adds a coffee to a user's coffee count, and stores the users and their
  coffee counts in PostgreSQL. A multi-module Gradle build runs ktlint, detekt, and the tests.

[0.2.3]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.3
[0.2.2]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.2
[0.2.1]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.1
[0.2.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.0
[0.1.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.0
