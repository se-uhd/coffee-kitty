# CLAUDE.md

## Project Overview

Coffee Kitty is a minimal coffee counter with the architecture of the coffee app of SE@UHD, the Software Engineering
Group at Heidelberg University. The package prefix `de.seuhd` stands for the group. Coffee Kitty is the running example
of a software engineering lecture, and students extend it over the semester. A user has a login name, an email address,
and a first and a last name. Each user has exactly one consumption, which holds a coffee count that starts at zero. The
REST API lists, creates, and reads users, reads a coffee count, and adds one coffee at a time. Every endpoint is open,
because the app has no authentication. The app has no frontend. In the dev profile, Swagger UI documents the API.
`GLOSSARY.md` defines one term for each concept. Use these terms in code, messages, and documentation.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters) architecture.
The included build `build-logic/` holds the convention plugins, and a subproject applies a convention plugin by its id
`de.seuhd.campuscoffee.<name>`. The root build and `build-logic/` read the same version catalog,
`gradle/libs.versions.toml`. `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md` records why the build
has this structure. The `detekt-rules` subproject holds the custom detekt rules. Every Maven repository of the build is
declared in `settings.gradle.kts` or `build-logic/settings.gradle.kts`, and `FAIL_ON_PROJECT_REPOS` fails the build
when a build script declares a repository.

### Module Dependencies

- `domain`: the domain models, the ports, and the business logic. Depends on the Spring Boot core, `spring-tx`, and
  kotlin-logging, never on Bean Validation, Jakarta Persistence (JPA), or the other modules.
- `api`: the controllers, the data transfer objects (DTOs), the DTO mappers, the exception handler, and the OpenAPI
  definition. Depends on `domain`.
- `data`: the JPA entities, the repositories, the entity mappers, the data adapters, and the Flyway migrations. Depends
  on `domain`.
- `application`: the composition root, which wires the other modules into one Spring Boot application. It holds the
  main class, the fixture loader with `FixturesProperties`, and `application.yaml`, but no web or business code.
  Depends on `domain`, `api`, and `data`.

### Layer Rules (Enforced by ArchUnit)

`ArchitectureTests` in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/architecture/` checks these rules:

- Only `application` may access `api` and `data`, and only `api`, `data`, and `application` may access `domain`.
- The production packages must be free of cycles.
- Every class, test classes included, must lie in a layer: `api..`, `domain..`, `data..`, or the application layer,
  which consists of the root package `de.seuhd.campuscoffee`, `configuration..`, and `tests..`. A class in a new
  production package of the application module fails the layer test until the package is added to
  `APPLICATION_PRODUCTION_PACKAGES`.
- The production classes of `api` and `application` must not depend on the data ports or on
  `domain/.../implementation/`.
- No class in `api` may depend on the internal ports or on `TestFixtures` in `domain/.../tests/`.
- The port packages may hold only interfaces named `*Service`, and `ports/data/` only interfaces named `*DataService`.
- No class in the controller package may be a plain `@Controller`.
- No production class, `*Impl` classes included, may depend on a concrete `*Impl` class. Depend on the port instead.
  The test `production code depends on ports, never on Impl types` exempts the `@Configuration` classes and ignores
  abstract classes, which lets each `*DataServiceImpl` extend the abstract base `CrudDataServiceImpl`.

### Ports and Adapters

The ports are in `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/`:

- API ports (`ports/api/`), which the controllers call, such as `UserService`.
- Data ports (`ports/data/`), which the data module implements, such as `UserDataService`. Each entity has a
  `*DataService` that extends the generic `CrudDataService<DOMAIN, ID>`.
- Internal ports (`ports/internal/`), which the fixture loader and the test setup call, such as `DataResetService`.

The implementations of the API and internal ports are in `domain/.../implementation/`. `UserServiceImpl.create` trims
the email address, converts it to lowercase, and checks the new user against the limits in the companion object of
`User`. In one transaction, the method then stores the user and the consumption of the user with a coffee count of
zero. The fixture loader and the test setup create the fixture users through `UserService.create`, which checks the
fixture users like any other new user. `CoffeeConsumptionServiceImpl.applyDelta` accepts only a delta of `+1` and reads
the consumption with `getByUserIdForUpdate`, which locks the row until the transaction ends. A second request that adds
a coffee for the same user at the same time waits until the first transaction ends. The second request then reads the
raised count, and both coffees count.

### Generic Base Classes

`CrudDataServiceImpl` is the base class of the data adapters. `CrudDataServiceImpl.upsert` inserts a domain object
without an id under a random `UUID`. For an object with an id, `upsert` updates the stored JPA entity through the entity
mapper, or throws a `NotFoundException` when no row has that id. A violated unique constraint becomes the
`DuplicationException` of the matching `ConstraintMapping.REGISTRY` entry.

The read methods of `CrudDataServiceImpl` and of the data adapters, except `getByUserIdForUpdate`, are not
`@Transactional`. For a caller without a transaction, these methods map the JPA entity outside any transaction. Because
`application.yaml` turns off Open Session in View, a lazy association loads only inside a transaction. A data adapter
for a JPA entity with a lazy association must therefore read and map the entity inside a `@Transactional` method.

The generic MapStruct interfaces `DtoMapper` and `EntityMapper` take the domain type first, as in
`UserDtoMapper : DtoMapper<User, UserDto>`. Every entity mapper overrides `updateEntity` to ignore the id and the
timestamps (see `UserEntityMapper`). Without the override, an update copies the id and the timestamps from the domain
object.

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
gradle build                                                 # compile, ktlint, detekt, and every test
gradle ktlintFormat                                          # apply the formatting fixes before a commit
gradle :domain:test --tests "CoffeeConsumptionServiceTest"   # one test class
gradle :application:test --tests "ArchitectureTests"         # the layer rules only, which need no Docker
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

Scope a `--tests` filter to one subproject, because Gradle fails the test task of every subproject without a matching
test. Quote the filter, because the backtick names of test methods contain spaces. In `:application:test`, a filter that
starts with `*` also runs the Cucumber scenarios, which need Docker. Without Docker, run the rest of the build with
`gradle build -x :application:test -x :data:test`.

The root `gradle.properties` turns on the Gradle configuration cache. A task action must not reference a top-level value
of its build script or a `Configuration`, or the build fails with a configuration cache problem. Copy such a value into
a local `val` first, and wrap a `Configuration` in `files(...)`.

In continuous integration (CI), `.github/workflows/build.yml` runs `scripts/check-version-sync.sh` and `gradle build` on
every push to `main` and on every pull request. After each push to `main`, `.github/workflows/dependency-submission.yml`
submits the runtime dependencies of the application to GitHub's dependency graph, which Dependabot alerts and security
updates read. Pin every action in a workflow (`uses:`) to a full commit SHA with a trailing `# vX.Y.Z` comment.

### Format, Lint, and Static Analysis

`gradle build` runs the `check` task, which runs ktlint and detekt and fails on any finding. ktlint applies the
`ktlint_official` code style with the settings of the root `.editorconfig`, and detekt applies
`config/detekt/detekt.yml` on top of the default configuration of detekt. Fix each finding, and do not add a detekt
baseline. detekt and ktlint run on their own Kotlin compilers, which the catalog entries `detekt-kotlin` and
`ktlint-kotlin` set independently of the `kotlin` entry. When you update detekt, set `detekt-kotlin` to the Kotlin
version that the new detekt release names. On a mismatch, detekt reports "detekt was compiled with Kotlin X".

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
3. Implement the plan in coherent steps, keep `gradle build` green, and reuse existing patterns and helpers.
4. Review the diff adversarially for bugs that pass `gradle build`, such as untested edge cases. Verify each finding
   against the code, apply the confirmed fixes, and run the build again.
5. Release the change (see Versioning and Releases).

Write each decision worth recording as a short architecture decision record (ADR) in `doc/adr/`, named
`NNN-meaningful-name.md` and based on `doc/adr/template.md`. Never edit an accepted ADR except for its status. Record a
changed decision in a new ADR that refers to the old one, and set the status of the old ADR to superseded.

## Versioning and Releases

The project follows [Semantic Versioning](https://semver.org/) and keeps a
[Keep a Changelog](https://keepachangelog.com/)-style `CHANGELOG.md`. For every notable change, add a bullet to its
group under `## [Unreleased]`, such as `Added`, `Changed`, or `Fixed`. A release raises the patch version for bug
fixes, tooling, and internal cleanups, the minor version for features, and the major version for breaking changes. The
`version` in the root `gradle.properties` must equal the version in the most recent `## [x.y.z]` header of
`CHANGELOG.md`, and `scripts/check-version-sync.sh` fails in CI when the two versions differ. To release a version:

1. For every open issue and pull request that `gh issue list` and `gh pr list` show, fix the issue or merge the pull
   request, or close it with a reason.
2. Move the bullets of `## [Unreleased]` under a new `## [x.y.z] - YYYY-MM-DD` header, add the link reference
   `[x.y.z]: https://github.com/se-uhd/coffee-kitty/releases/tag/vX.Y.Z` at the bottom of `CHANGELOG.md`, and set
   `version` in `gradle.properties` to the new version.
3. Commit these changes as the release commit and push the commit to `main`.
4. When CI passes on the release commit, tag the commit and push the tag with
   `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z`.

## Commit Messages

Write each commit message for a reader of `git log` who has no access to your notes. Give it a concise imperative
subject line and a body that explains the reason for the change and any decisions that are not obvious. Never cite
references that are not in the repository, such as the ids of review findings or ticket shorthands. Group related work
into a few cohesive commits. End every commit message that Claude Code writes with the `Co-Authored-By:` trailer from
the harness instructions.

## Database

The app stores its data in PostgreSQL through Spring Data JPA, and Flyway applies the migrations in
`data/src/main/resources/db/migration/`. At startup, Hibernate checks that every mapped table and column exists with a
compatible type (`ddl-auto: validate`), and it never changes the schema. The check ignores the NOT NULL, CHECK, and
unique constraints and every column that no JPA entity maps. An INSERT or UPDATE that violates one of these
constraints, such as a null in a NOT NULL column, passes the startup check and fails when it runs.

- Write the migrations in plain DDL (data definition language) without comments. Explain a column or a constraint in
  the KDoc of its JPA entity.
- Never edit a migration that others have applied, such as a pushed or released one. Add a new migration instead.
  Because Flyway compares the checksum of every applied migration at startup, any change to an applied file, even to
  a comment, stops the application from starting. You may still edit a migration that only your own dev database has
  applied, but then remove that database with `docker rm -f -v campus-coffee-db` and start a new container with the
  `docker run` command above.
- Once the app runs in a rolling deployment, which replaces the running instances one at a time, keep each migration
  compatible with the previous release. The instances of the previous release keep serving requests while the next
  release migrates the schema. A renamed or dropped column, or a new NOT NULL column without a default, then ships in
  two releases. The first release adds and backfills any new column, and the second release removes the old column or
  adds the NOT NULL constraint.

## Testing Strategy

- Unit tests: the domain service tests in `domain/src/test/kotlin/`, such as `UserServiceTest`, and
  `FixtureStartupLoaderTest` in `application/src/test/kotlin/`. They use Mockito mocks.
- Integration tests: `CrudDataServiceIntegrationTest` and `ConstraintMappingRegistryIntegrationTest` in
  `data/src/test/kotlin/`. They extend `AbstractDataIntegrationTest`, which runs each test in the Spring context of
  `DataTestApplication` against PostgreSQL in Testcontainers. `ConstraintMappingRegistryIntegrationTest` fails unless
  `ConstraintMapping.REGISTRY` lists exactly the unique constraints and unique indexes of the schema.
- System tests: the classes in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/system/`, such as
  `UserSystemTests`. They extend `AbstractSystemTest` and send requests with Spring's `RestTestClient`.
  `SystemTestUtils` holds the PostgreSQL container from Testcontainers that the system and acceptance tests share.
- Acceptance tests: Cucumber scenarios for behavior-driven development (BDD). The step definitions and the Cucumber
  configuration are in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/acceptance/`, and the feature files,
  such as `consumption.feature`, are in `application/src/test/resources/de/seuhd/campuscoffee/tests/acceptance/`.
- Architecture tests: `ArchitectureTests` (see Layer Rules).

### Test Naming

A test method (annotated with `@Test` or `@ParameterizedTest`) has a Kotlin backtick name that reads as a sentence in
active voice and present tense. The name states the subject under test first and then, as a fact, the outcome that the
test asserts, such as an HTTP status, an exception type, or a returned value. Avoid `should`. Examples are
``fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`()`` and
``fun `applyDelta with a delta other than +1 throws ValidationException and writes nothing`()``. Other functions
keep camelCase names.

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

`application/src/main/resources/application.yaml` has a default block and a `dev` block. `FixturesProperties` binds the
fixture flags `campus-coffee.fixtures.load-on-startup` and `campus-coffee.fixtures.reset-on-startup`. When
`load-on-startup` is true, Spring registers `FixtureStartupLoader`, which creates the fixture users in a database
without users. When `reset-on-startup` is true as well, the fixture loader first runs a data reset and creates the
fixture users on every start. The `dev` block turns on both flags and sets the datasource to
`localhost:${DB_PORT:5433}`, the server port to `${SERVER_PORT:8081}`, and the springdoc paths. In the dev profile, the
API is at `http://localhost:8081/api` and Swagger UI at `http://localhost:8081/api/swagger-ui.html`. Outside the dev
profile, springdoc is off.

## Important Patterns

- Error handling: The domain exceptions are `NotFoundException` (404) and `DuplicationException` (409), which the data
  adapters throw (see Generic Base Classes), and `ValidationException` (400), which the domain services and
  `UserController` throw. `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler`, which maps Spring's
  standard web exceptions to their status codes (e.g., 404 for an unknown path). Any other exception becomes a 500.
  The body of every error response is an `ErrorResponse`. Its `errorCode` is the class name of the domain exception
  or, for any other error, the reason phrase of the status without spaces (e.g., `BadRequest`). To add a domain
  exception, make the changes that the KDoc of `GlobalExceptionHandler.handleMappedException` lists.
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
  offset. Read the clock only with `LocalDateTime.now(ZoneOffset.UTC)`, never in the system time zone. A client
  converts a timestamp to a local time zone only for display.

## Adding a New Entity

In this order, `gradle build` passes after each step. The ports come first, because a port without an implementation
does not break the build. The data adapter (step 7) comes before the implementation of the API port (step 8), because
a `@Service` needs a bean for each port that it uses.

1. Create the domain model in `domain/.../model/`. The domain model implements `DomainModel<UUID>` and has a nullable
   `id`, `createdAt`, and `updatedAt` (see `User`).
2. Create the API port in `domain/.../ports/api/`. An operation that only the fixture loader or the test setup calls
   goes on an internal port in `domain/.../ports/internal/`.
3. Create the data port in `domain/.../ports/data/` (extend `CrudDataService<DOMAIN, UUID>`).
4. In one step, create the Flyway migration in `data/src/main/resources/db/migration/`, the JPA entity in
   `data/.../persistence/entities/` (extend `Entity`), and a `ConstraintMapping.REGISTRY` entry for each unique
   constraint. Hibernate checks every JPA entity against the schema at startup, and
   `ConstraintMappingRegistryIntegrationTest` fails while the registry and the unique constraints of the schema differ
   (see Testing Strategy). Write the annotation of the JPA entity fully qualified as `@jakarta.persistence.Entity`,
   because an import of `jakarta.persistence.Entity` would hide the base class `Entity`. Choose the `ON DELETE` action
   of each foreign key. Use `CASCADE` only for a row that belongs to the referenced row, such as a consumption, which
   belongs to its user. Under the default `NO ACTION`, deleting a referenced row throws a
   `DataIntegrityViolationException`, and the client gets a 500. An operation that deletes a row must therefore check
   for references first and, for a referenced row, throw a new domain exception that `GlobalExceptionHandler` maps to
   409 Conflict.
5. Create the repository in `data/.../persistence/repositories/` (extend `JpaRepository`).
6. Create the entity mapper in `data/.../mapper/` (extend `EntityMapper` and override `updateEntity` as described in
   Generic Base Classes). Map each reference to another entity by its id (see `CoffeeConsumptionEntityMapper`).
   Add a lookup such as `userReference` to `EntityReferenceResolver`, list the resolver and the referenced mapper in
   `@Mapper(uses = ...)`, and put `@Mapping(target = "<reference>", source = "<reference>.id")` on `toEntity` and
   `updateEntity`. Without these mappings, MapStruct maps the nested domain object, such as the `User` in a
   `CoffeeConsumption`, through the referenced mapper, and an update writes the fields of the nested domain object over
   the referenced row.
7. Create the data adapter in `data/.../implementations/` (extend `CrudDataServiceImpl`).
8. Create the implementation of the API port in `domain/.../implementation/`.
9. Clear the data of the new entity in `DataResetServiceImpl.clearAll`. Place the call after the calls for the entities
   that reference the new entity and before the calls for the entities that the new entity references.
10. Create the DTO in `api/.../dtos/` and the DTO mapper in `api/.../mapper/`. The DTO implements `Dto<UUID>` and has
    a nullable `id`, `createdAt`, and `updatedAt`, which the OpenAPI definition marks as read-only (see `UserDto`).
11. Create the controller in `api/.../controller/`. An endpoint that creates an object rejects a body with an `id` and
    returns 201 Created with a `Location` header (see `UserController.create`).
12. Add tests: a domain unit test modeled on `UserServiceTest`, cases in `CrudDataServiceIntegrationTest`, and a system
    test class that extends `AbstractSystemTest`. The integration cases include an update without timestamps, which
    fails when the entity mapper copies the timestamps.
13. Update Ports and Adapters in this file for the new ports, and add the new terms to `GLOSSARY.md`. List the new
    endpoints in the REST API section of `README.md`, with one runnable curl block per endpoint. Update the texts that
    summarize the entities, such as the README introduction, the description in `OpenApiConfig`, and the glossary
    entries "data reset" and "domain exception".
