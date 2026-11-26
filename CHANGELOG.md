# Changelog

This file lists the notable changes of each release of Coffee Kitty in the format of
[Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/). The version numbers follow
[Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- The requirement FR-19 (record an expense for a user) in `requirements/`: An admin records an expense for a user, and
  the daily limit of FR-6 does not refuse it. Coffee Kitty does not implement it yet.
- The subproject `models`, in which TLC checks a TLA+ model of the coffee count of a user and a model of concurrent
  requests (ADR 012). `doc/statecharts/` holds a chart of the first model.
- `gradle check` fails when a rule of a TLA+ model breaks, a chart in `doc/statecharts/` is out of date, or a replay
  test finds a step of a model that `CoffeeConsumptionServiceImpl` does not take.
- An Alloy model of the domain model (`models/src/main/alloy/DomainModel.als`). `gradle check` fails when a class, a
  property, or a value rule of `domain/.../model/` differs from the model or the model breaks an assertion.
- A class diagram of the Alloy model in `doc/class-diagrams/`, which `gradle :models:classDiagrams` writes with the
  multiplicities that the Alloy Analyzer finds. `gradle check` fails when the diagram differs from the model.
- `models/README.md` describes the models and the diagrams.
- Rules for comments, enforced by detekt (ADR 013): KDoc is required only on contracts and has at most 6 lines on a
  type and 3 elsewhere, and other comments are only `// ADR-NNN` or `// External: <link> <reason>`.
- detekt applies all its rules to the tests and the test fixtures of each module, and `gradle detektBuildLogic` to
  `build-logic` and the Gradle scripts (ADR 023).
- The Kotlin compiler fails on a warning in every compilation and Gradle script (ADR 023).
- `detektMain`, `detektTest`, and `detektTestFixtures` apply the rules of `config/detekt/type-resolution.yml` with
  type resolution, such as `UnusedImport`, and `gradle check` runs them (ADR 023).
- `config/markdown/limits.yml` with a limit for each Markdown file, which `gradle markdownLimits` checks (ADR 013).
- `scripts/check-commit-message.sh` checks Conventional Commits with a body of 1 to 5 bullets (ADR 013). CI runs it, and
  `mise install` enables it as a Git hook (`mise.toml`).
- `gradle markdownLimits` checks the structure and the section lengths of each ADR against `doc/adr/template.md`
  (ADR 016).
- ADRs 020 to 022 record the reasons of the foreign keys, the JPA mapping, and the content type of error responses
  that the cleanup of the comments removed. ADR-010 and ADR-014 regain the reasons of daemon flags and ktlint blocks.
- The OpenAPI definition documents the error responses of every endpoint, and a system test checks them against the
  statuses that the tests receive (ADR 019).
- The detekt rule `OpenApiTextLength` limits the summaries and descriptions of the OpenAPI annotations (ADR 018).
- `gradle ruleIndex` writes the lists of the rule files in CLAUDE.md and AGENTS.md from the front matter of the rules,
  and `gradle check` fails when a list differs from the rules.
- Claude Code hooks in `.claude/settings.json` check a commit message and the Markdown limits right after an edit.
- Path-scoped rules for agents in `.claude/rules/`, which Claude Code loads for the files that they name.
- ADR 014 records the reasons of the build settings, and ADR 015 what the server log records about a request.

### Changed

- Every endpoint needs an acting user unless its controller function has the new annotation `OpenEndpoint` (ADR 011).
  The status codes of the existing endpoints stay the same.
- A `HEAD` request needs an acting user wherever the `GET` request needs one.
- FR-6 resolves its TBD on a daily limit: A user who is not an admin records at most 5 expenses and 50.00 € on one day,
  configurable up to 100.00 €.
- The daily limit replaces the limit of 100.00 € for a single expense of such a user. GLOSSARY.md defines the term daily
  limit.
- The upper limit of 1,000.00 € for a single amount is a rule (MUST) in FR-4, FR-6, and FR-17. Until now, these
  requirements stated it as a plausibility check (SHOULD). `CoffeePrice.amountCents` has the constraint for it.
- PostgreSQL refuses to delete a user with a balance, because the foreign key of `user_balance` has `NO ACTION` instead
  of `CASCADE` (migration V13, ADR-020).
- FR-3, FR-9, and FR-13 state 401 Unauthorized for an unknown login name in `X-Acting-User`, and FR-9 states 404 Not
  Found for an unknown user. Supporting statements in FR-6 and FR-8 no longer contradict their requirements.
- The OpenAPI definition states `application/json` as the media type of every response.
- The models are optional: `gradle build` leaves out the subproject `models`, and `gradle -Pmodels build` checks them.
  CI checks them only in se-uhd/coffee-kitty, so that an assignment needs no TLA+ or Alloy (ADR 012).
- CLAUDE.md has 168 instead of 639 lines. The guidance for single directories moved into `.claude/rules/`.
- The README has 199 instead of 366 lines and points to Swagger UI for the REST API.
- The code keeps KDoc only where ADR 013 requires it or the code cannot say it, and no other comments.
- CHANGELOG.md follows Keep a Changelog 1.1.0 with bullets of at most 2 lines.
- FR-6 has 101 instead of 122 lines, and a requirement has at most 110 lines (ADR 018). A review compared FR-6
  statement by statement with the old version.
- GLOSSARY.md defines each term after ISO 704, in tables with the identifiers and the source of each term.
- The history of `main` follows Conventional Commits.
- An accepted ADR that a later ADR changes in part has the status "Accepted, amended by [ADR-NNN](NNN-name.md)"
  (ADR 017).

### Removed

- The annotation `ActingUserRequired` (ADR 011).
- The detekt rules `UndocumentedFunction`, `UndocumentedNonLocalClass`, and `UndocumentedEnumConstructorProperty`,
  which required KDoc on every function and type.

### Fixed

- `POST /api/users` returns 400 Bad Request for an email address with a control character (FR-14). Until now, the NUL
  character caused a 500, and other control characters that are not whitespace, such as U+0001, were stored.
- The OpenAPI definition states the rules on control characters for the names, the email address, and the note of a
  correction.
- The message of a `DataConstraintException` for a value that its column cannot store, such as a text with the NUL
  character, names the SQL state instead of "the constraint (unnamed)".
- `gradle build -x :application:test` no longer fails because the coverage report reads the output of
  `:application:bootBuildInfo` without depending on it.
- The detekt rule `UndocumentedFunctionParameter` finds an `@param` tag after a tag whose text starts with a code span.
  The Kotlin PSI merged the two tags, so the rule reported the second parameter as undocumented.

### Security

- The PostgreSQL driver 42.7.15 and Hibernate 7.4.17.Final replace the versions of Spring Boot 4.1.2, which have six
  high advisories and an SQL injection in the `json_*` HQL functions (GHSA-v5mr-59hq-7mvm).

## [0.3.1] - 2026-11-25

### Fixed

- The domain models of the later features declare Bean Validation constraints on `Payment.note`, `CoffeeBean.name`,
  `CoffeeRating.value`, and `CoffeePrice.amountCents`. `ModelConstraintsTest` checks them.

## [0.3.0] - 2026-11-25

### Added

- An admin can correct the coffee count of a user at any time with `PUT /api/users/{userId}/consumption` and a note
  (FR-8). The migration V5 creates the table `count_corrections`, which stores each correction.
- `GET /api/users/{userId}/consumption/corrections` lists the corrections of a user, newest first. Both endpoints of
  FR-8 return 403 Forbidden when the acting user is not an admin.
- The migrations V6 to V11 create the tables of later features, which no endpoint uses yet: `coffee_prices`, `payments`,
  `coffee_beans`, `coffee_ratings`, `expenses`, `user_balance`, and `kitty_balance`.
- The domain models `CoffeePrice`, `Payment`, `CoffeeBean`, and `CoffeeRating`, each with a data port and a data
  adapter. The tables `expenses`, `user_balance`, and `kitty_balance` have only a JPA entity and a repository so far.
- The section "Tables Without an Endpoint" in `CLAUDE.md` lists what each later feature still needs, and GLOSSARY.md
  defines the new terms.
- The requirements FR-4 (record a deposit), FR-10 (raise the kitty balance), and FR-6 (record own expenses) in
  `requirements/`, which Coffee Kitty does not implement yet.
- Each requirement lists the requirements that it depends on and those that depend on it. A requirement ID has no
  suffix, so FR-4b and FR-5b are now FR-10 and FR-11.
- The requirements FR-13 (add a coffee) and FR-14 (create a user) for functions that Coffee Kitty already implements.
- The requirements FR-15 (report a balance), FR-16 (report the kitty balance), and FR-17 (set the price per cup), which
  Coffee Kitty does not implement yet.
- The requirement FR-18 (charge or credit a correction), which resolves the TBD of FR-11 on the balance after a
  correction. Coffee Kitty does not implement it yet.
- The requirements FR-5 (pay for a coffee) and FR-11 (credit an undone coffee) in `requirements/`, which Coffee Kitty
  does not implement yet.
- The migration V12 adds the column `coffee_times.price_cents`, which stores the amount that an undo of the coffee
  credits to the balance (FR-11). No JPA entity maps the column yet.
- The requirement FR-12 (rate a bean) in `requirements/`, which follows the coffee app of SE@UHD. Coffee Kitty does not
  implement it yet.
- A client can read the undo status of a user with `GET /api/users/{userId}/consumption/undo`, which returns `undoable`
  and `undoableUntil` (FR-9). GLOSSARY.md defines the term undo status.
- `GracePeriod.closesAt` returns the end of a grace period, and `ConsumptionRequests.retrieveUndoStatus` sends the
  request of FR-9 for the system tests.
- `TestFixtures` has further test fixtures: `admin()`, `regularUser()`, `FIXED_TIME`, `fixedClock()`, and three
  consumptions, such as `consumptionWithRecentCoffee`. GLOSSARY.md defines the term test fixture.
- The unit tests `ConsumptionPropertiesTest`, `IdentifiableTest`, and `DomainExceptionsTest` of the domain, and further
  tests in `GracePeriodTest`, `CoffeeConsumptionServiceTest`, and `UserServiceTest`.
- `StartupTest` checks that the app creates no user on startup without a configured bootstrap admin.
- Integration tests of the data module: `CoffeeConsumptionDataServiceIntegrationTest` checks the times of the coffees
  and a negative coffee count, and `CrudDataServiceIntegrationTest` the order and the roles of the users.
- Further system tests, such as an undo at a coffee count of zero, an undo with a grace period of zero
  (`UndoWithoutGracePeriodSystemTests`), and login names of 1, 255, and 256 characters.
- `FailureSystemTests` checks that a request returns 500 and stores nothing when a data adapter throws an exception.
- The undo status (FR-9) has no tests yet, because they are the reference solution of assignment 7. Until 2026-12-08,
  the coverage gate and PIT leave out its functions, such as `GracePeriod.closesAt`.
- `gradle build` creates a coverage report of all modules with JaCoCo. The build fails below 90% of the lines or 80% of
  the branches, and when the tests do not execute every function of a domain service or a controller.
- `gradle :domain:pitest` runs mutation tests of the domain services with PIT and fails when the tests detect fewer than
  90% of all mutants. `gradle build` does not run it, and the CI workflow runs it after the build.
- The system and acceptance tests check the JSON field names of the API (ADR 007). They send each request body as a map
  and parse each response into a class of `Json.kt` instead of a DTO.
- Architecture tests check that the data module uses only the models, the exceptions, and the data ports of the domain
  and that the production classes contain no test fixtures.
- Architecture tests check that a controller function that reads the acting user has the annotation `ActingUserRequired`
  and that the system and acceptance tests use no class of the api module.
- ADRs 002 to 006 in `doc/adr/` record the decisions on Spring in the domain, the role checks, the read methods without
  a transaction, the test fixtures, and the validation.
- ADRs 007 to 009 record the decisions on the tests of the JSON field names, the required fields of a request, and the
  value rules on the domain models. ADR 010 records the measurement behind each threshold of the build.
- `gradle build` runs CPD, the copy-paste detector of PMD, on the production code and fails at a duplicate of 70 tokens.

### Changed

- Breaking: The api module checks only the structure of a request body, and the domain checks every value (ADR 006). A
  value that breaks a rule gets a 400 with the error code `ValidationException`.
- Breaking: The error code changes from `BadRequest` to `ValidationException` for an invalid login name, a blank first
  or last name, a negative total of a correction, and a note that is blank or longer than 500 characters.
- A request DTO states a required field with a property that is not nullable, such as `val delta: Int`, and Jackson
  rejects a body without the field (ADR 008).
- Breaking: A 400 for a missing field or a value of the wrong JSON type names only the first such field: "The field
  emailAddress is missing or has a value that the server cannot read."
- The domain models declare their value rules as Bean Validation constraints, such as `@Size` on `User.loginName`. A
  domain service checks them with the new `DomainValidator` before it stores an object (ADR 009).
- A correction with an invalid value for an unknown user returns 404 instead of 400.
- `gradle :domain:pitest` no longer creates mutants that remove the null checks that the Kotlin compiler generates. It
  runs with the default timeout and the default number of threads of PIT.
- Every limit in the code and in the build names its source, such as a requirement, a standard, or a measurement.
  Dependabot uses its default limit of open pull requests.
- The limit of 254 characters for an email address cites RFC 5321, and the limit of 255 characters for a first or last
  name is an open TBD in FR-14.
- The requirements quote the input of the team assistant for each of their numbers and state the responses to refused
  requests below the Statement.
- Each open TBD states what the requirement specifies until the contact person answers. The sentences below a Statement
  use MUST and MUST NOT for a rule, SHOULD for a plausibility check, and MAY for an optional input.
- FR-8 states the time of a correction, the list of corrections, and the effect of a correction to a higher coffee
  count, and resolves the TBD on the length of a note.
- FR-12 states that a coffee has at most one rating.
- FR-4, FR-6, and FR-17 state the limits of an amount and of a weight as plausibility checks, with at most 100.00 € for
  a user and 1,000.00 € for an admin. GLOSSARY.md defines the term plausibility check.
- Breaking: A request body with an unknown field returns 400 with the error code `BadRequest`. Until now,
  `POST /api/users` returned `ValidationException` for an `id` and ignored any other unknown field.
- `POST /api/users` reads the new `CreateUserDto`, and `UserDto` is only the response body. The OpenAPI definition marks
  every field of a response body as required. Until now, it marked none.
- The production jar contains neither the user fixtures nor code that deletes data (ADR 005). `TestFixtures` is in the
  test fixtures of the domain module (`src/testFixtures`).
- The Spring bean `DatabaseCleaner` in the test fixtures of the data module replaces `DataResetService` and truncates
  every table, including `expenses`, `user_balance`, and `kitty_balance`.
- `gradle :application:bootTestRun` starts the dev application with the user fixtures.
  `gradle :application:bootRun --args='--spring.profiles.active=dev'` now keeps the data of the previous run.
- A data adapter throws the new `DataConstraintException` instead of `DataIntegrityViolationException` for a violated
  CHECK constraint or a deletion that a foreign key prevents. The client still gets a 500.
- `CountCorrectionDataService` has only `insert` and `getAllByUserId`, because a stored correction never changes.
- `SecurityConfig` reads the endpoints that need an acting user from the controller functions with the new annotation
  `ActingUserRequired` instead of a list of paths.
- The unit tests of the api module are in `api/src/test`.
- The message of a 409 for an undo after the grace period is "The grace period of the most recent coffee has passed."
- The message for an invalid first or last name states the rules in parallel form. The OpenAPI definition describes the
  path parameter `userId` as "The id of the user."
- The data adapters are in the package `data.implementation`, as the domain services are in `domain.implementation`.
  Until now, the package was `data.implementations`.
- GLOSSARY.md defines the term endpoint, and the documentation no longer uses the word route for it. The message of a
  missing object writes "id" in lowercase ("User with id ... does not exist.").
- GLOSSARY.md calls a user of `TestFixtures` a user fixture instead of a fixture user, and the code and the
  documentation use this term.
- The tests check observable behavior, such as a status or the stored state, and no longer check implementation details,
  such as the wording of a message.
- The tests run in the time zone Asia/Tokyo, so that a test fails when the production code uses the time zone of the JVM
  for a time in UTC.
- The domain exceptions extend the sealed class `DomainException`.

### Removed

- Bean Validation in the api module, such as `@NotNull` on the DTOs and `@Valid` in the controllers (ADR 008).
- Breaking: The upper limit of 1,000,000 for a corrected coffee count (FR-8). The only limit is 2,147,483,647, the
  largest count that Coffee Kitty can store, at which adding a coffee returns 409 Conflict.
- The interfaces `Dto` and `DtoMapper` of the api module.
- `CrudDataService.clear`, `FixtureStartupLoader`, `FixturesProperties`, and the keys
  `campus-coffee.fixtures.load-on-startup` and `campus-coffee.fixtures.reset-on-startup`.
- Tests that only repeated another test.
- The code of `GlobalExceptionHandler` for cases that cannot occur.

### Fixed

- Every error message is in English. Until now, `POST /api/users` with `Accept-Language: de` returned a message that
  mixed German and English.
- The message of a 401 for a blank `X-Acting-User` header is "The header X-Acting-User is missing or blank." Until now,
  the message was "The header X-Acting-User is missing."

## [0.2.3] - 2026-11-22

### Fixed

- A request with a wildcard in its `Content-Type`, such as `*/*` or `application/*`, gets a 415 with the error code
  `UnsupportedMediaType` instead of a 500.

## [0.2.2] - 2026-11-22

### Fixed

- A request body with a JSON string for a number or boolean field gets a 400 with the error code `BadRequest`. Until
  now, `{"delta": "1"}` in `POST /api/users/{userId}/consumption` added a coffee.
- A request body with a JSON number for an enum field or with a number or boolean for a `String` field gets a 400 with
  the error code `BadRequest`. Until now, `"role": 1` created an admin, and `"loginName": 12345` was stored as text.
- The REST API reads and writes only JSON. A request with a YAML body gets a 415 with the error code
  `UnsupportedMediaType` instead of a 500, and `Accept: application/yaml` gets a 406 instead of a YAML response.

## [0.2.1] - 2026-11-17

### Added

- GLOSSARY.md defines the terms correction and note of FR-8.
- `SecurityConfig` requires an acting user for the endpoints of FR-8, `PUT /api/users/{userId}/consumption` and
  `GET /api/users/{userId}/consumption/corrections`. Until Coffee Kitty implements FR-8, an acting user gets 405 or 404.

### Changed

- The step definitions of FR-3 in `CucumberConsumptionSteps` send their requests through `ConsumptionRequests` instead
  of building HTTP requests, and their Then steps check the stored coffee count.
- The system tests send their requests through the new `UserRequests` and new functions of `ConsumptionRequests`, which
  return a `Response` with the status code, the headers, and the body or the error response.

### Fixed

- FR-8 names the TBD of FR-3 on corrections by its question instead of a wrong position and states the decision of the
  group lead as its business requirement.
- The Rationale of FR-8 no longer specifies who reads the notes, which a TBD of FR-8 leaves open.

## [0.2.0] - 2026-11-17

### Added

- A user can undo their most recent coffee with `POST /api/users/{userId}/consumption/undo` within its grace period
  (FR-3). The key `campus-coffee.consumption.grace-period` sets the grace period, 5 minutes by default.
- The migration V3 creates the table `coffee_times` with the time of each recent coffee. A user cannot undo a coffee
  added before this release.
- Every user has a role, `USER` or `ADMIN` (migration V4), and the user fixture `jane_doe` is an admin.
  `POST /api/users` may set the role of the new user, which defaults to `USER`.
- Spring Security resolves the acting user from the header `X-Acting-User` with `ActingUserAuthenticationFilter`, and
  `SecurityConfig` lists the endpoints that need one.
- A request without a known acting user gets 401 Unauthorized before Spring MVC reads its body. Every 401 has the header
  `WWW-Authenticate: X-Acting-User realm="Coffee Kitty"` (RFC 9110).
- FR-8 in `requirements/FR-8-correct-coffee-count.md` specifies the correction of a coffee count by an admin, which the
  group lead decided and assignment 5 implements. FR-3 marks its TBD on corrections as resolved.
- The step definitions send their requests, such as adding a coffee, through `ConsumptionRequests`, which takes login
  names and returns status codes and values. It also sends the requests of FR-8, which assignment 5 implements.
- When no admin exists, `BootstrapAdminLoader` stores the bootstrap admin on startup or gives the role `ADMIN` to the
  user with the configured login name.
- The environment variables `BOOTSTRAP_ADMIN_LOGIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL_ADDRESS`, `BOOTSTRAP_ADMIN_FIRST_NAME`,
  and `BOOTSTRAP_ADMIN_LAST_NAME` set the keys under `campus-coffee.bootstrap-admin`.

### Changed

- Breaking: Only an admin may create a user. `POST /api/users` returns 401 Unauthorized without a known acting user in
  the header `X-Acting-User` and 403 Forbidden when the acting user is not an admin.
- Breaking: The migration V4 gives every existing user the role `USER`, including `jane_doe` in a database that the
  `dev` profile does not reset. Configure the bootstrap admin before you create users.
- The firewall of Spring Security returns 400 Bad Request for a path with `;`, `//`, `..`, or an encoded slash and for a
  header value with a control character.
- The README describes how the group buys coffee beans, from the kitty or as the expense of a user, and how to set up
  IntelliJ IDEA. It states that students first see the app in the lectures on agentic software engineering.
- The setup instructions of the README start with `mise trust` and `mise install`, and every command in the README has
  its own code block.
- `gradle check` also runs ktlint on the root `build.gradle.kts` and `settings.gradle.kts` and on the included build
  `build-logic`. This release fixes four violations that ktlint found in `java-conventions.gradle.kts`.
- Coffee Kitty uses Gradle 9.9 instead of 9.8 (`mise.toml`), and `mise install` installs it. In IntelliJ IDEA, select
  the `gradle-9.9.x` directory as the local Gradle installation (README, section IntelliJ IDEA).
- For a failed test, Gradle prints the assertion message, such as `expected: 200 but was: 404`, and the stack trace with
  the class and method of each line, which IntelliJ IDEA links to the source.
- The workflow `.github/workflows/mise-outdated.yml` checks the pins in `mise.toml` every Monday, because Dependabot
  does not read `mise.toml`. The workflow opens an issue when a pin no longer covers the newest release of its tool.
- When IntelliJ IDEA runs a single scenario of a feature file, Cucumber loads the step definitions from the package of
  `CucumberTests` (`application/src/test/resources/cucumber.properties`) and no longer scans every test class.

### Removed

- The instruction of the README to clone the repository.

### Fixed

- A successful build no longer ends with the deprecation notice of detekt 2.0.0-alpha.6. Until a detekt release includes
  the fix from detekt/detekt#9577, `org.gradle.warning.mode=none` hides all Gradle deprecation warnings.
- IntelliJ IDEA could not start the Gradle daemon on a JDK older than 23. `gradle/gradle-daemon-jvm.properties` now
  requires JDK 25 for the daemon, which Gradle downloads when the machine has none.

## [0.1.0] - 2026-10-31

### Added

- A REST API that lists and creates users, adds a coffee to a user's coffee count, and stores the users and their coffee
  counts in PostgreSQL. A multi-module Gradle build runs ktlint, detekt, and the tests.

[Unreleased]: https://github.com/se-uhd/coffee-kitty/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.3.1
[0.3.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.3.0
[0.2.3]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.3
[0.2.2]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.2
[0.2.1]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.1
[0.2.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.0
[0.1.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.0
