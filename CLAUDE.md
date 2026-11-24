# CLAUDE.md

## Project Overview

Coffee Kitty is a minimal coffee counter with the architecture of the coffee app of SE@UHD, the Software Engineering
Group at Heidelberg University. The package prefix `de.seuhd` stands for the group. Coffee Kitty is the running example
of a software engineering lecture, and students extend it over the semester. A user has a login name, an email address,
and a first and a last name. Each user has exactly one consumption, which holds a coffee count that starts at zero. The
REST API lists, creates, and reads users, reads a coffee count, and adds one coffee at a time. It undoes the most recent
coffee within 5 minutes and reads the undo status, which states whether that undo is possible. An admin can also correct
a coffee count with a note and list the corrections of a user.

`requirements/` holds the functional requirements, such as FR-3 for the undo, FR-8 for the correction, and FR-9 for the
undo status. FR-4, FR-5, FR-6, FR-10, FR-11, and FR-12 specify deposits, the price per cup, expenses, and ratings, which
the app does not implement yet. Each requirement follows the template of the lecture and has one ID. Never change or
reuse an ID. An ID has no suffix such as `b`. A requirement lists the requirements that it depends on under Supporting
Information, and each of those lists it in return. FR-7 was deleted and stays unused.

Every user has a role, `USER` or `ADMIN`, and only an admin may create a user, correct a coffee count, or list the
corrections. The app has no authentication yet: The header `X-Acting-User` of a request contains the login name of the
acting user. `ActingUserAuthenticationFilter` in `api/.../security/` resolves the login name in this header to a `User`
before the controllers run, and `CurrentUserProvider` returns that user to the controllers. The controllers pass that
user to the domain, which checks the role with `requireAdmin` in `AuthorizationUtil.kt`, as the coffee app of SE@UHD
does. The app has no frontend. In the dev profile, Swagger UI documents the API. The data module already holds the
tables of later features, such as the price per cup, deposits, expenses, beans, ratings, and balances. No endpoint uses
these tables yet (see Tables Without an Endpoint). `GLOSSARY.md` defines one term for each concept. Use these terms in
code, messages, and documentation.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters) architecture.
The included build `build-logic/` holds the convention plugins, and a subproject applies a convention plugin by its id
`de.seuhd.campuscoffee.<name>`. The root build and `build-logic/` read the same version catalog,
`gradle/libs.versions.toml`. `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md` records why the build
has this structure. The `detekt-rules` subproject holds the custom detekt rules, and the `coverage` subproject creates
the coverage report of all modules. `settings.gradle.kts` and `build-logic/settings.gradle.kts` declare every Maven
repository of the build, and `FAIL_ON_PROJECT_REPOS` fails the build when a build script declares a repository.

### Module Dependencies

- `domain`: the domain models, the ports, and the business logic. Depends on the Spring Boot core and `spring-tx`,
  never on Bean Validation, Jakarta Persistence (JPA), or the other modules.
- `api`: the controllers, the data transfer objects (DTOs), the DTO mappers, the exception handler, and the OpenAPI
  definition. Depends on `domain`.
- `data`: the JPA entities, the repositories, the entity mappers, the data adapters, and the Flyway migrations. Depends
  on `domain`.
- `application`: the composition root, which wires the other modules into one Spring Boot application. It holds the
  main class, the startup task (`StartupDataInitializer`, which runs `BootstrapAdminLoader` with
  `BootstrapAdminProperties`), `ClockConfiguration`, and `application.yaml`, but no web or business code. Its test
  sources hold the dev application (see Configuration).
  Depends on `domain`, `api`, and `data`.

### Layer Rules (Enforced by ArchUnit)

`ArchitectureTests` in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/architecture/` checks these rules:

- Only `application` may access `api` and `data`, and only `api`, `data`, and `application` may access `domain`.
- The production packages must be free of cycles.
- Every class, test classes included, must lie in a layer: `api..`, `domain..`, `data..`, or the application layer,
  which consists of the root package `de.seuhd.campuscoffee`, `configuration..`, `tests..`, and `dev..`. A class in a
  new production package of the application module fails the layer test until you add the package to
  `APPLICATION_PRODUCTION_PACKAGES`.
- The production classes of `api` and `application` must not depend on the data ports or on
  `domain/.../implementation/`.
- No production class in `api` may depend on the internal ports.
- Within the domain module, the production classes of `data` may depend only on the models, the exceptions, and the data
  ports. The rule lists these packages, so `data` cannot depend on a new package of the domain until the rule names it.
- A controller function that calls `CurrentUserProvider.currentUser` must have the annotation `ActingUserRequired` (see
  Important Patterns).
- No class under `tests..` may depend on a class of the api module, such as a DTO (see Testing Strategy).
- The production classes must not contain the test fixtures. `PRODUCTION_CLASSES` leaves out the jars with the test
  fixtures of the domain and data modules. If it does not, this rule fails.
- The port packages may hold only interfaces named `*Service`, and `ports/data/` only interfaces named `*DataService`.
- No production class, `*Impl` classes included, may depend on a concrete `*Impl` class. Depend on the port instead.
  The test `production code depends on ports, never on Impl types` exempts the `@Configuration` classes and ignores
  abstract classes, which lets each `*DataServiceImpl` extend the abstract base `CrudDataServiceImpl`.

### Ports and Adapters

The ports are in `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/`:

- API ports (`ports/api/`), which the controllers call, such as `UserService`.
- Data ports (`ports/data/`), which the data module implements, such as `UserDataService`. The data port of an entity
  that the domain may change and delete extends the generic `CrudDataService<DOMAIN, ID>`. The data port of an entity
  that never changes after the app stores it declares only the operations that the domain needs. For example,
  `CountCorrectionDataService` has only `insert` and `getAllByUserId`. No domain service uses the data ports of the
  price, the payments, the beans, and the ratings yet (see Tables Without an Endpoint).
- Internal ports (`ports/internal/`), which the bootstrap admin loader, the test setup, and the dev application call:
  `UserInternalService` creates a user without an acting user.

The implementations of the API and internal ports are in `domain/.../implementation/`. `UserServiceImpl.create` trims
the email address, converts it to lowercase, and checks the new user against the limits in the companion object of
`User`. In one transaction, the function then stores the user and the consumption of the user with a coffee count of
zero. The test setup and the dev application create the fixture users through `UserInternalService.create`, which checks
the fixture users like any other new user.

`CoffeeConsumptionServiceImpl.applyDelta` accepts only a delta of `+1` and reads the consumption with
`getByUserIdForUpdate`, which locks the row until the transaction ends. A second request that adds a coffee for the same
user at the same time waits until the first transaction ends. The second request then reads the raised count, so the
coffee count includes both coffees. `applyDelta` also appends the time of the coffee to `coffeeTimes` and drops the
times of the coffees outside their grace period. The data module stores the list in the table `coffee_times`.
`CoffeeConsumptionServiceImpl.undo` locks the consumption in the same way, lowers the count by one if
`GracePeriod.isOpen` accepts the last of the `coffeeTimes`, and removes that time. Otherwise, it throws a
`ConflictException`. After an undo, the user can undo the coffee before while its own grace period is open.
`CoffeeConsumptionServiceImpl.undoableUntil` applies the checks of `undo` without a lock and without changing the
consumption. It returns `GracePeriod.closesAt` for the last of the `coffeeTimes` when an undo would succeed, and null
otherwise. `ConsumptionController` serves this result as the undo status (`UndoStatusDto`). `applyDelta` and
`GracePeriod` read the time from the `Clock` bean of `ClockConfiguration`. `ConsumptionProperties` binds the grace
period, `campus-coffee.consumption.grace-period`, with a default of 5 minutes.

`CoffeeConsumptionServiceImpl.correctCount` lets an admin set the coffee count to a new total with a note (FR-8). It
checks the role with `requireAdmin`, refuses a total outside 0 to `CountCorrection.MAX_TOTAL` and a note that is
missing, blank, or longer than `CountCorrection.MAX_NOTE_LENGTH`, and locks the consumption like `applyDelta`. A total
below the count drops the times of the newest coffees from `coffeeTimes`, as the coffee app of SE@UHD removes the newest
coffees of the user from its undo stack. A higher total adds coffees without a time. In the same transaction, the
function calls `insert` of the data port `CountCorrectionDataService` to store a `CountCorrection` with the count before
and after, the note, and the admin. The data module stores the corrections in the table `count_corrections`, which
references the corrected user with `ON DELETE CASCADE` and the admin with the default `NO ACTION`. `getCorrections` also
checks the role with `requireAdmin` and returns the corrections of a user, newest first.

### Generic Base Classes

`CrudDataServiceImpl` is the base class of the data adapters. `CrudDataServiceImpl.upsert` inserts a domain object
without an id under a random `UUID`. For an object with an id, `upsert` updates the stored JPA entity through the entity
mapper, or throws a `NotFoundException` when no row has that id. A violated unique constraint becomes the
`DuplicationException` of the matching `ConstraintMapping.REGISTRY` entry. For any other violated constraint, `upsert`
and `delete` throw a `DataConstraintException` of the domain module, so that no exception of Spring or Hibernate crosses
a data port. Examples of such a constraint are a CHECK constraint and a foreign key that prevents a deletion. A
`DataConstraintException` is not a `DomainException` and becomes a 500, because it means that a domain service did not
check a rule before it called the data port.

The read functions of `CrudDataServiceImpl` and of the data adapters, except `getByUserIdForUpdate`, are not
`@Transactional`. For a caller without a transaction, these functions map the JPA entity outside any transaction.
Because `application.yaml` turns off Open Session in View, a lazy association loads only inside a transaction. A data
adapter for a JPA entity with a lazy association must therefore read and map the entity inside a `@Transactional`
function.

The generic MapStruct interface `EntityMapper` takes the domain type first, as in
`UserEntityMapper : EntityMapper<User, UserEntity>`. Every entity mapper overrides `updateEntity` to ignore the id and the timestamps (see `UserEntityMapper`).
Without the override, an update copies the id and the timestamps from the domain object.

### Tables Without an Endpoint

The migrations V6 to V11 create the tables of later features, which the coffee app of SE@UHD has, and V12 adds the
column `coffee_times.price_cents`. No endpoint and no domain service uses them yet, and their columns and constraints
follow that app without its event log. The KDoc of each JPA entity explains the columns, the CHECK constraints, and the
`ON DELETE` action of each foreign key.

- The price per cup, the payments, the beans, and the ratings have the full data layer: a domain model, a data port, an
  entity mapper, and a data adapter. The domain models are `CoffeePrice`, `Payment`, `CoffeeBean`, and `CoffeeRating`.
  The table `coffee_prices` holds at most one row. A row of `payments` is a deposit with a user or a kitty adjustment
  without a user. A row of `coffee_beans` has a unique name among the live beans and, for a merged bean, the id of the
  bean that an admin merged it into. A row of `coffee_ratings` holds a score from 1 to 5 of a bean by a user.
  `EntityReferenceResolver.beanReference` resolves the bean of a rating. An API port, a domain service, DTOs, and a
  controller are missing.
- The expenses (`expenses`) and the balances (`user_balance` and `kitty_balance`) have only a JPA entity and a
  repository with the queries and locks that a data adapter needs. A domain model, a data port, an entity mapper, and a
  data adapter are missing. `ExpenseEntity.expenseType` is a `String` with the values `BEANS` and `OTHER`, which
  MapStruct can map to the constants of a domain enum with these names. `UserBalanceEntity` and `KittyBalanceEntity` do
  not extend `Entity`, because their tables have neither a random id nor timestamps. `EntityMapper` and
  `CrudDataServiceImpl` accept only subclasses of `Entity` and do not apply to these two classes.
- No migration stores a price, and every test starts with empty tables. `CoffeePriceDataService.findCurrent` then
  returns null. The startup and the test setup have to store the start price, as `BootstrapAdminLoader` stores the
  bootstrap admin.
- Coffee Kitty cannot compute a balance from the stored data afterwards, because it does not store the price of each
  past coffee. Every operation that moves money therefore has to change the balances in the same transaction. Examples
  are adding or undoing a coffee, a correction, a deposit, and an expense. A domain service locks a row with
  `UserBalanceRepository.findForUpdateByUserId` or `KittyBalanceRepository.findForUpdateById` through its data adapter,
  as `applyDelta` locks a consumption. No migration inserts a row. The first operation that changes a balance has to
  insert its row, unless `UserServiceImpl.create` creates the row of a new user together with the consumption.
- An undo has to raise the balance by the amount by which the coffee lowered it, also when the price per cup changed in
  between (FR-11). The column `coffee_times.price_cents` (migration V12) is for this amount. No JPA entity maps it yet.
  The implementation of FR-5 has to store the price with the time of each coffee, and the implementation of FR-11 has to
  use it. Coffee Kitty drops the time and the price of a coffee outside its grace period when the user adds a coffee. A
  correction that removes such a coffee therefore cannot use the price of that coffee. FR-11 has an open TBD on the
  amount by which a correction changes the balance.

### Naming and File Conventions

- An interface that crosses modules is a port. Name it `*Service`, or `*DataService` for a data port, and name its
  implementation `<Port>Impl`, such as `UserService` and `UserServiceImpl`. Code depends on the port, never on the
  implementation (see Layer Rules). An interface used within one module may have any name.
- The name of an implementation never contains the name of a library or vendor (`UserDataServiceImpl`, not
  `JpaUserDataService`). When a port has several implementations, name each implementation after its behavior or role.
- Name a stateless helper without injected dependencies `*Util`, and a JPA entity `*Entity`.
- Each file holds one top-level type and may add related top-level functions and constants (e.g., `errorCodeFor` in
  `ErrorResponse.kt`). Only a tightly bound sealed hierarchy may put several types in one file.

## Build and Run Commands

Run Gradle in a shell with mise activated or as `mise exec -- gradle ...`, because the project has no Gradle wrapper and
mise installs Java and Gradle from `mise.toml`. The `java` entry in `gradle/libs.versions.toml` sets the Java toolchain
and the Kotlin `jvmTarget`, and `mise.toml` pins the same major version by hand. The dev database and the
Testcontainers tests need Docker.

```shell
gradle build                                                 # compile, ktlint, detekt, CPD, every test, coverage gate
gradle ktlintFormat                                          # apply the formatting fixes before a commit
gradle :domain:test --tests "CoffeeConsumptionServiceTest"   # one test class
gradle :application:test --tests "ArchitectureTests"         # the layer rules only, which need no Docker
gradle :coverage:testCodeCoverageReport                      # the coverage report of all modules
gradle :domain:pitest                                        # mutation testing of the domain services
gradle cpd                                                   # duplicates in the production code
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
gradle :application:bootTestRun                              # the dev application with the fixture users
```

Scope a `--tests` filter to one subproject, because Gradle fails the test task of every subproject without a matching
test. Quote the filter, because the backtick names of test functions contain spaces. In `:application:test`, start the
filter with a class name or a package, as in `--tests "ArchitectureTests"` or
`--tests "de.seuhd.campuscoffee.tests.system*"`, and never with `*`. With `--tests "*ArchitectureTests"`, the matching
tests run, but the task fails with `CucumberTests > initializationError`. Gradle then also passes the suite class
`CucumberTests` to JUnit, and the filter removes all its scenarios. JUnit fails a suite without tests
(`NoTestsDiscoveredException`). Without Docker, run the rest of the build with
`gradle build -x :application:test -x :data:test -x :coverage:coverageGate`. The coverage gate needs the execution data
of every test.

The root `gradle.properties` turns on the Gradle configuration cache. A task action must not reference a top-level value
of its build script or a `Configuration`, or the build fails with a configuration cache problem. Copy such a value into
a local `val` first, and wrap a `Configuration` in `files(...)`.

In continuous integration (CI), `.github/workflows/build.yml` runs `scripts/check-version-sync.sh` and `gradle build` on
every push to `main` and on every pull request. After each push to `main`, `.github/workflows/dependency-submission.yml`
submits the runtime dependencies of the application to GitHub's dependency graph, which Dependabot alerts and security
updates read. Pin every action in a workflow (`uses:`) to a full commit SHA with a trailing `# vX.Y.Z` comment.

### Format, Lint, and Static Analysis

`gradle build` runs the `check` task, which runs ktlint and detekt and fails on any finding. `check` also runs the
coverage gate and CPD (see Code Coverage, Mutation Testing, and Duplicate Code). ktlint applies the `ktlint_official`
code style with the settings of the root `.editorconfig`, and detekt applies `config/detekt/detekt.yml` on top of the
default configuration of detekt. Fix each finding, and do not add a detekt baseline. detekt and ktlint run on their own
Kotlin compilers, which the catalog entries `detekt-kotlin` and `ktlint-kotlin` set independently of the `kotlin` entry.
When you update detekt, set `detekt-kotlin` to the Kotlin version with which the new detekt release was built. On a
mismatch, detekt reports "detekt was compiled with Kotlin X".

The custom rule set `campus-coffee-kdoc` is in `detekt-rules/src/main/kotlin/de/seuhd/campuscoffee/detekt/`, and
`gradle :detekt-rules:test` runs its tests. The rule set requires KDoc on every non-local class, interface, object,
and enum class, except companion objects, and on every non-local function that is not an override. A public
function that is not an override needs an `@param` for every parameter, and a non-private constructor property of an
enum class needs an `@property` or `@param`. No detekt rule checks the test sources, because the `detekt` task checks
only `src/main/kotlin`.

## Development Workflow

Scale the depth of each review to the change, from a light review for a small fix to a thorough review for a feature.
A substantial change goes through these steps:

1. Explore the code and write a concrete plan with the files to touch, the approach, and how to verify the change.
   Before you go on, resolve open decisions with the requester.
2. Review the plan adversarially against the code. Look for wrong assumptions, missed edge cases, and regressions,
   verify each finding in the source, and revise the plan with the confirmed findings.
3. Implement the plan in coherent steps, check that `gradle build` passes after each step, and reuse existing patterns
   and helpers.
4. Review the diff adversarially for bugs that pass `gradle build`, such as untested edge cases. Verify each finding
   against the code, apply the confirmed fixes, and run the build again. After a change to the domain module, also run
   `gradle :domain:pitest`.
5. Release the change (see Versioning and Releases).

Write each decision worth recording as a short architecture decision record (ADR) in `doc/adr/`, named
`NNN-meaningful-name.md` and based on `doc/adr/template.md`. Never edit an accepted ADR except for its status. Record a
changed decision in a new ADR that refers to the old one, and set the status of the old ADR to superseded.

## Versioning and Releases

The project follows [Semantic Versioning](https://semver.org/), and `CHANGELOG.md` has the format of [Keep a
Changelog](https://keepachangelog.com/). For every notable change, add a bullet to its group under
`## [Unreleased]`, such as `Added`, `Changed`, or `Fixed`. A release raises the patch version for bug fixes, tooling,
and internal cleanups, the minor version for features, and the major version for breaking changes. As long as the major
version is 0, a breaking change raises the minor version, which Semantic Versioning allows for initial development. The
`version` in the root `gradle.properties` must equal the version in the most recent `## [x.y.z]` header of
`CHANGELOG.md`, and `scripts/check-version-sync.sh` fails in CI when the two versions differ. To release a version:

1. For every open issue and pull request that `gh issue list` and `gh pr list` show, fix the issue or merge the pull
   request, or close it with a reason.
2. Move the bullets of `## [Unreleased]` under a new `## [x.y.z] - YYYY-MM-DD` header, add the link reference
   `[x.y.z]: https://github.com/se-uhd/coffee-kitty/releases/tag/vX.Y.Z` at the bottom of `CHANGELOG.md`, and set
   `version` in `gradle.properties` to the new version.
3. Run `gradle build` and `gradle :domain:pitest`. Continue only when both pass.
4. Commit these changes as the release commit and push the commit to `main`.
5. When CI passes on the release commit, tag the commit and push the tag with
   `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z`.

## Commit Messages

Write each commit message for a reader of `git log` who cannot read your notes. Give it a concise imperative subject
line and a body that explains the reason for the change and any decisions that are not obvious. Never cite references
that are not in the repository, such as the ids of review findings or ticket shorthands. Group related work into a few
cohesive commits. End every commit message that Claude Code writes with the `Co-Authored-By:` trailer from the harness
instructions.

## Database

The app stores its data in PostgreSQL through Spring Data JPA, and Flyway applies the migrations in
`data/src/main/resources/db/migration/`. At startup, Hibernate checks that every mapped table and column exists with a
compatible type (`ddl-auto: validate`), and it never changes the schema. The check ignores the NOT NULL, CHECK, and
unique constraints and every column that no JPA entity maps. An INSERT or UPDATE that violates one of these
constraints, such as a null in a NOT NULL column, passes the startup check and fails when it runs.

- Write the migrations in plain DDL (data definition language) without comments. Explain a column or a constraint in
  the KDoc of its JPA entity.
- Never edit a migration that others have applied, such as a pushed or released one. Add a new migration instead.
  Because Flyway compares the checksum of every applied migration at startup, any change to an applied file, even to a
  comment, stops the app from starting. You may still edit a migration that Flyway has applied only to your own dev
  database. Then remove that database with `docker rm -f -v campus-coffee-db` and start a new container with the
  `docker run` command above.
- Once the app runs in a rolling deployment, which replaces the running instances one at a time, keep each migration
  compatible with the previous release. The instances of the previous release keep serving requests while the next
  release migrates the schema. A renamed or dropped column, or a new NOT NULL column without a default, then ships in
  two releases. The first release adds and backfills any new column, and the second release removes the old column or
  adds the NOT NULL constraint.

## Testing Strategy

Test each part of the code at the level that can detect its faults:

- Controllers have no unit tests. Keep a controller as thin as possible: It reads the request, calls an API port, and
  maps the result to a DTO. System and acceptance tests check it.
- Write unit tests primarily for code with logic of its own: the domain module, helper functions, and a mapper with
  significant logic. The system tests check a mapper that only copies properties.
- Write integration tests for the data module, for the behavior that only the database can show, such as constraints,
  queries, locks, and `ON DELETE` actions. System tests also run the data adapters, so an integration test does not
  repeat what a system test checks. A table without an endpoint has only its integration tests.

The tests are in these places:

- Unit tests: the tests in `domain/src/test/kotlin/`, such as `UserServiceTest` and `GracePeriodTest`; the tests of the
  api module in `api/src/test/kotlin/`, such as `RequestRejectedExceptionHandlerTest`; and the tests of the startup task
  in `application/src/test/kotlin/`, such as `BootstrapAdminLoaderTest`. The test of a class that depends on ports, such
  as a domain service, replaces the ports with Mockito mocks. The test of a mapper with significant logic creates the
  implementation that MapStruct generates, such as `UserDtoMapperImpl`, with its constructor. `StartupTest` starts the
  startup task in a Spring context with `application.yaml` and a mocked port.
- Integration tests: the classes in `data/src/test/kotlin/`. `CrudDataServiceIntegrationTest` tests the base class and
  the data adapters of the users, the consumptions, and the corrections. `CoffeeConsumptionDataServiceIntegrationTest`
  has further tests of the data adapter of the consumptions. `UserDataServiceIntegrationTest` tests the limits of the
  database for the length of an email address. Each data adapter of Tables Without an Endpoint has a test class of its
  own, such as `PaymentDataServiceIntegrationTest`. `ExpenseAndBalanceRepositoryIntegrationTest` tests the tables
  without a data port through their repositories. The integration tests extend `AbstractDataIntegrationTest`, which runs
  each test in the Spring context of `DataTestApplication` against PostgreSQL in Testcontainers and cleans the database
  before each test (see Test Fixtures).
- System tests: the classes in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/system/`, such as
  `UserSystemTests`. They extend `AbstractSystemTest`, which creates a `UserRequests` and a `ConsumptionRequests` for
  the server port of its Spring context. These classes in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/`
  send the requests with Spring's `RestTestClient`. Each of their functions returns a `Response` with the status code,
  the headers, and the body or the error response. `SystemTestUtils` holds one client per server port and the PostgreSQL
  container from Testcontainers that the system and acceptance tests share. The system tests run with the system clock.
  `UndoWithoutGracePeriodSystemTests` sets the grace period to zero with `@TestPropertySource` and therefore runs in a
  Spring context of its own. `FailureSystemTests` also runs in a Spring context of its own, because Mockito spies wrap
  two data adapters, so that a test can make one of their functions fail.
- Acceptance tests: Cucumber scenarios for behavior-driven development (BDD). The step definitions and the Cucumber
  configuration are in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/acceptance/`, and the feature files,
  such as `consumption.feature`, are in `application/src/test/resources/de/seuhd/campuscoffee/tests/acceptance/`.
  `CucumberSpringConfiguration` replaces the `Clock` bean with a `SettableClock`, which the steps set to the times in
  the scenarios, such as 10:05:01. It also provides the `ConsumptionRequests` of the steps. A step names a user by login
  name and looks up the id with `ConsumptionRequests.userId`.
- Architecture tests: `ArchitectureTests` (see Layer Rules).

System tests and step definitions send every request through `UserRequests` and `ConsumptionRequests` and never build an
HTTP request themselves, so that a reader of a test does not need to know the REST API. The exception is
`ErrorResponseSystemTests`, in which the request itself is under test. It builds the requests for an unknown path, for a
path that the firewall rejects, and with a `Content-Type` or `Accept` header that the API does not support.

The system and acceptance tests are clients of the REST API and use no class of the api module, which an architecture
test checks. A request body is a map with the JSON field names, such as `mapOf("delta" to 1)`. The tests parse a
response into a class of `Json.kt`, such as `UserJson`, with a mapper that fails on an unknown or a missing field. A
timestamp is a `String` that `utc` parses. If a test used a DTO for both directions, a renamed JSON field would change
the request of the test and the response of the server together, and the test would still pass. When an endpoint gets a
new field, add it to the class in `Json.kt`. The names of the header `X-Acting-User` and of its challenge are literals
in `SystemTestUtils` for the same reason.

### Test Fixtures

`TestFixtures` in `domain/src/testFixtures/kotlin/de/seuhd/campuscoffee/domain/tests/` holds the test objects that the
tests of every module share: the fixture users with `admin()` and `regularUser()`, the fixed time `FIXED_TIME` with
`fixedClock()`, and the consumptions of `consumptionWithoutCoffee`, `consumptionWithRecentCoffee`, and
`consumptionWithOldCoffee`. Take a test object from `TestFixtures` when a test needs one of these objects. Create the
object in the test class when the tests need values that these objects do not have, such as an id and timestamps.
Prepare the state that each test of a class needs anew, such as a stored user, in a function with `@BeforeEach` (see
`CoffeeConsumptionDataServiceIntegrationTest.storeUser`).

`DatabaseCleaner` in `data/src/testFixtures/kotlin/de/seuhd/campuscoffee/data/tests/` deletes the rows of every table
before each integration test, system test, and scenario and on every start of the dev application. It reads the names
of the tables from the PostgreSQL catalog and truncates them in one statement, so a new table needs no change to it.
`DatabaseCleanerIntegrationTest` checks every table that the catalog lists.

`TestFixtures` and `DatabaseCleaner` are in the Gradle source set `src/testFixtures` of their module (plugin
`java-test-fixtures`). The other modules add them with `testImplementation(testFixtures(project(":domain")))`. The
production jar contains neither the fixture users nor code that deletes data, so no configuration of a deployed app can
delete its data.

### Test Naming

A test function (annotated with `@Test` or `@ParameterizedTest`) has a Kotlin backtick name that reads as a sentence in
active voice and present tense. The subject and the verb come first, and the condition follows. The name contains one
outcome that the test asserts, such as an HTTP status, an exception type, or a returned value. Do not add that a refused
operation changes nothing, although the test asserts it. Avoid `should`, and use a verb instead of a noun
(`deleting a user`, not `delete of a user`). Examples are ``fun
`undo throws ConflictException for a coffee added one hour ago`()`` and ``fun
`posting a delta of 2 returns 400 Bad Request`()``. Other functions have camelCase names.

A test checks behavior that a caller can observe, such as the status, the error code, the exception type, or the stored
state. It never checks an implementation detail, that is, anything that can change while the behavior stays the same.
Examples are the wording of a message, an annotation, a configuration setting, the calls between domain classes, and the
number and order of calls. It does not check a framework either, such as the messages of Bean Validation or the rule of
Spring that a lock needs a transaction. A test may check which values a message contains or leaves out, such as the name
of an invalid field, a rejected value, or a login name in a log message.

### Code Coverage, Mutation Testing, and Duplicate Code

JaCoCo records the execution data of the tests of every module. The subproject `coverage` merges the data into one
report at `coverage/build/reports/jacoco/testCodeCoverageReport/html/index.html`, because the system and acceptance
tests of the application module cover most of the other modules. The task `coverageGate`, which `check` runs, has two
rules (`coverage/build.gradle.kts`). The first rule fails the build when the tests cover less than 95% of the lines or
85% of the branches of all modules together. The class `Application` and the generated mappers do not count. Raise these
minimums as the coverage grows, and never lower them to make a build pass. A ratio over all modules cannot detect a
single function without a test, so the second rule requires that a test executes every function of a domain service and
of a controller.

`gradle :domain:pitest` runs the mutation testing tool PIT on the classes in `domain/.../implementation/`. PIT changes
the code, e.g., by negating a condition, and runs the unit tests of the domain module against each changed version
(mutant). A mutant that no test detects points to a missing assertion or to an assertion that passes although the
behavior is wrong. The report is in `domain/build/reports/pitest/index.html`. The task fails when the tests detect fewer
than 90% of all mutants (`pitest-conventions.gradle.kts`). This share also counts the mutants in code that no unit test
executes, so a function of a domain service without a unit test lowers it. `check` does not run PIT, and CI runs it
after the build. Run it before you push a change to the domain module, and add a test or improve an assertion for each
surviving mutant that changes the behavior. The mutants that survive today are null checks that the Kotlin compiler
generates.

`check` runs CPD, the copy-paste detector of PMD, on the production code. It fails at a duplicate of 70 tokens, which
is a copy of about 10 to 13 lines (`build.gradle.kts`). `gradle cpd` runs it alone. Extract the shared code instead of
raising the threshold.

## Key Technologies

- Spring Boot and Kotlin on Java, with the versions in `gradle/libs.versions.toml` and `mise.toml`.
- Spring Data JPA with Hibernate, PostgreSQL, and Flyway.
- MapStruct through kapt (the Kotlin annotation processing tool), Bean Validation for the request DTOs, and springdoc
  for the OpenAPI definition and Swagger UI.
- JUnit, Mockito with mockito-kotlin, AssertJ, Testcontainers, Cucumber, and ArchUnit for the tests.
- ktlint for the formatting, and detekt with the `campus-coffee-kdoc` rule set for the static analysis.

## REST API Endpoints

The endpoints accept and return only JSON. `ApiWebConfig` adds the prefix `/api` to the path of every controller, and
the controllers leave the prefix out of their `@RequestMapping`. The REST API section of `README.md` lists every
endpoint with its status codes and a runnable curl command.

## Configuration

`application/src/main/resources/application.yaml` has a default block and a `dev` block. The `dev` block sets the
datasource to `localhost:${DB_PORT:5433}`, the server port to `${SERVER_PORT:8081}`, and the springdoc paths. In the dev
profile, the API is at `http://localhost:8081/api` and Swagger UI at `http://localhost:8081/api/swagger-ui.html`.
Outside the dev profile, springdoc is off.

`gradle :application:bootTestRun` starts the dev application: the app in the dev profile with the fixture users. Its
`main` function is in `application/src/test/kotlin/de/seuhd/campuscoffee/dev/DevApplication.kt` and adds
`DevDataConfiguration` to the app. On every start, `DevDataSeeder` cleans the database with the `DatabaseCleaner` and
creates the fixture users before the web server accepts requests. The dev application is in the test sources, because
it uses the test fixtures, which the production jar does not contain. `gradle :application:bootRun` with
`--args='--spring.profiles.active=dev'` starts the app in the dev profile on the data of the previous run, without
cleaning the database.

## Important Patterns

- Acting user: Spring Security runs `ActingUserAuthenticationFilter` before the controllers. A controller function that
  calls `CurrentUserProvider.currentUser` needs the annotation `ActingUserRequired`. `SecurityConfig` reads the HTTP
  method and the path of every function with this annotation from Spring MVC and requires authentication for these
  routes. Without the annotation, a request that has no acting user and lacks a required field gets 400 instead of 401.
  The reason is that Spring MVC validates the body before the controller calls `CurrentUserProvider.currentUser`. An
  architecture test fails for a controller function that calls `currentUser` without the annotation.
- Firewall: The default firewall of Spring Security rejects, among others, a path with `;`, `//`, `..`, or an encoded
  slash and a header value with a control character. `SecurityConfig.requestRejectedHandler` and
  `RequestRejectedExceptionHandler` respond to such a request with 400 and an `ErrorResponse` (`writeErrorResponse` in
  `api/.../security/`).
- Error handling: The data adapters throw the domain exceptions `NotFoundException` (404) and `DuplicationException`
  (409), as described in Generic Base Classes. `CoffeeConsumptionServiceImpl.undo` throws `ConflictException` (409),
  `requireAdmin` throws `ForbiddenException` (403), and the domain services throw `ValidationException` (400).
  `JsonAuthenticationEntryPoint` responds to a request without a known acting user with 401 and the header
  `WWW-Authenticate`, which RFC 9110 requires in every 401. `GlobalExceptionHandler` extends
  `ResponseEntityExceptionHandler`, which maps Spring's standard web exceptions to their status codes (e.g., 404 for an
  unknown path). An `IllegalArgumentException` for a wildcard in the `Content-Type` header becomes a 415, and any other
  exception becomes a 500. The body of every error response is an `ErrorResponse`. Its `errorCode` is the class name of
  the domain exception or, for any other error, the reason phrase of the status without spaces (e.g., `BadRequest`). To
  add a domain exception, create a subclass of the sealed class `DomainException` and add its status to
  `GlobalExceptionHandler.statusOf`, which does not compile without it.
- Validation: The api module checks the structure of a request body, and the domain checks its values. A request DTO
  has nullable properties and only `@NotNull` constraints, and its `@Schema` annotations document the limits of the
  domain in the OpenAPI definition. A body with a missing field, a value of the wrong JSON type, or an unknown field
  gets a 400 with the error code `BadRequest`. A domain service checks every rule for a value, such as a length or a
  range, and throws a `ValidationException` (400). These rules then also hold for a user that no request creates, such
  as the bootstrap admin. Do not add a constraint such as `@Size` or `@Pattern` to a DTO.
- Request bodies: Jackson rejects a value of the wrong JSON type instead of converting it, and the request gets a 400
  with the error code `BadRequest`. It also rejects a field that the DTO does not have
  (`spring.jackson.deserialization.fail-on-unknown-properties`), such as an `id` in the body of `POST /api/users`. The
  `spring.jackson` settings in `application.yaml` reject a number with a fraction for an integer field, a JSON string
  for a number or a boolean field, and a JSON number for an enum field. `JsonConfig` in `api/.../configuration/` rejects
  a JSON number or boolean for a `String` field, because Spring Boot has no property for this setting. A new DTO gets
  these checks without any code. The API responds only in English: `spring.web.locale-resolver` in `application.yaml`
  fixes the locale, so that the messages of Bean Validation do not depend on the header `Accept-Language`.
- MapStruct: The `kotlin-kapt-conventions` plugin runs MapStruct through kapt with the option
  `mapstruct.unmappedTargetPolicy=ERROR`. With this option, the build fails when a target property has neither a
  source nor `ignore = true`.
- IDE configuration metadata: Declare every custom key in a `*Properties` class with KDoc, from which the IDE resolves
  the key in `application.yaml`. `application` depends on `data` at compile scope, not `runtimeOnly`, so that the IDE
  also resolves the keys of a `*Properties` class in the data module.
- Ids and timestamps: `PersistableEntity` implements `Persistable<UUID>`, which lets the repository insert a new JPA
  entity with the id from `upsert` without a preceding SELECT. `Entity` sets the timestamps in `@PrePersist` and
  `@PreUpdate` and truncates them to microseconds, because a PostgreSQL `timestamp` column stores microseconds. A
  timestamp that `upsert` returns then equals the timestamp that a later query reads.
- Time: Every timestamp in the database, the code, and the API is in UTC, and the API serves timestamps without an
  offset. A domain service that compares times reads the current time from the injected `Clock`, so that a test can fix
  the time (see `CoffeeConsumptionServiceTest`). Other code reads the current time with
  `LocalDateTime.now(ZoneOffset.UTC)`, never in the system time zone. A client converts a timestamp to a local time zone
  only for display. The tests run in the time zone Asia/Tokyo (`java-conventions.gradle.kts`), so that a test fails when
  the production code uses the time zone of the JVM for a time in UTC. In UTC, the time zone of GitHub Actions, the same
  test would pass.

## Adding a New Entity

In this order, `gradle build` passes after each step. The ports come first, because a port without an implementation
does not break the build. The data adapter (step 7) comes before the implementation of the API port (step 8), because
a `@Service` needs a bean for each port that it uses. For the tables of Tables Without an Endpoint, steps 4 and 5 are
done. For the price, the payments, the beans, and the ratings, steps 1, 3, 6, and 7 are done as well, and the next step
is the API port (step 2).

1. Create the domain model in `domain/.../model/`. The domain model implements `DomainModel<UUID>` and has a nullable
   `id`, `createdAt`, and `updatedAt` (see `User`).
2. Create the API port in `domain/.../ports/api/`. An operation that only the startup tasks or the test setup calls
   belongs to an internal port in `domain/.../ports/internal/`.
3. Create the data port in `domain/.../ports/data/`. Extend `CrudDataService<DOMAIN, UUID>` if the domain may change and
   delete the objects. For objects that never change after the app stores them, declare only the operations that the
   domain needs (see `CountCorrectionDataService`).
4. In one step, create the Flyway migration in `data/src/main/resources/db/migration/`, the JPA entity in
   `data/.../persistence/entities/` (extend `Entity`), and a `ConstraintMapping.REGISTRY` entry for each unique
   constraint. Hibernate checks every JPA entity against the schema at startup. Without its registry entry, a violated
   unique constraint becomes a 500 instead of a 409. Write the annotation of the JPA entity fully qualified as
   `@jakarta.persistence.Entity`, because an import of `jakarta.persistence.Entity` would hide the base class `Entity`.
   Choose the `ON DELETE` action of each foreign key. Use `CASCADE` only for a row that belongs to the referenced row,
   such as a consumption, which belongs to its user. Under the default `NO ACTION`, deleting a referenced row throws a
   `DataConstraintException`, and the client gets a 500. An operation that deletes a row must therefore check
   for references first and, for a referenced row, throw a new domain exception that `GlobalExceptionHandler` maps to
   409 Conflict.
5. Create the repository in `data/.../persistence/repositories/` (extend `JpaRepository`).
6. Create the entity mapper in `data/.../mapper/` (extend `EntityMapper` and override `updateEntity` as described in
   Generic Base Classes). Map each reference to another entity by its id (see `CoffeeConsumptionEntityMapper`). Add a
   lookup such as `userReference` to `EntityReferenceResolver`, list the resolver and the referenced mapper in
   `@Mapper(uses = ...)`, and put `@Mapping(target = "<reference>", source = "<reference>.id")` on `toEntity` and
   `updateEntity`. Without these mappings, MapStruct maps the nested domain object, such as the `User` in a
   `CoffeeConsumption`, through the referenced mapper. An update then writes the fields of the nested domain object over
   the referenced row.
7. Create the data adapter in `data/.../implementations/` (extend `CrudDataServiceImpl`).
8. Create the implementation of the API port in `domain/.../implementation/`.
9. Create the DTOs in `api/.../dtos/` and the DTO mapper in `api/.../mapper/`. A response DTO has only non-null
   properties, including `id`, `createdAt`, and `updatedAt`, and marks each as required for the OpenAPI definition with
   `@field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)` (see `UserDto`). A request DTO has nullable properties
   with `@NotNull` and no property for a value that the server assigns (see `CreateUserDto` and Validation in Important
   Patterns).
10. Create the controller in `api/.../controller/` and keep it thin (see Testing Strategy). An endpoint that creates an
    object returns 201 Created with a `Location` header (see `UserController.create`). Put `ActingUserRequired` on a
    function that needs the acting user.
11. Add tests: a domain unit test modeled on `UserServiceTest`, an integration test class for the data adapter, such as
    `PaymentDataServiceIntegrationTest`, and a system test class that extends `AbstractSystemTest`. The integration
    tests include an update without timestamps, which fails when the entity mapper copies the timestamps. Add the
    functions for the requests to a class such as `UserRequests` and the JSON bodies to `Json.kt`.
12. Update Ports and Adapters in this file for the new ports, and add the new terms to `GLOSSARY.md`. List the new
    endpoints in the REST API section of `README.md`, with one runnable curl block per endpoint. Update the texts that
    summarize the entities, such as the README introduction, the description in `OpenApiConfig`, and the glossary
    entry "domain exception".
