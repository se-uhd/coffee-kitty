# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Coffee Kitty is a minimal coffee counter with the architecture of the coffee app of SE@UHD (the Software Engineering
Group at Heidelberg University, hence the `de.seuhd` package). A software engineering lecture uses it as its running
example, and students extend it over the semester. A user has a login name, an email address, and a first and a last
name, and each user has one consumption with a coffee count that starts at zero. The REST API lists, creates, and reads
users, reads a coffee count, and adds one coffee at a time. Every endpoint is open, because Coffee Kitty has no
authentication. There is no frontend, and Swagger UI in the dev profile documents the API. `GLOSSARY.md` defines the
vocabulary. Use its terms in code, messages, and docs.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters)
architecture. The included build `build-logic/` holds the convention plugins, which the modules apply by the id
`de.seuhd.campuscoffee.<name>`, and `gradle/libs.versions.toml` is the version catalog of the root build and of
`build-logic/`. `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md` records why the build has this
structure. The `detekt-rules` subproject holds the custom detekt rules.

### Module Dependencies

- `domain`: the models, the port interfaces, and the business logic. It depends on the Spring Boot core, `spring-tx`,
  and kotlin-logging, and never on Bean Validation, Jakarta Persistence (JPA), or the api, data, or application
  modules.
- `api`: the controllers, the data transfer objects (DTOs), the DTO mappers, the exception handler, and the OpenAPI
  definition. Depends on domain.
- `data`: the JPA entities, the repositories, the entity mappers, the data port implementations, and the Flyway
  migrations. Depends on domain.
- `application`: the composition root with `Application`, `FixtureStartupLoader`, `FixturesProperties`, and
  `application.yaml`, and no web or business code. Depends on domain, api, and data.

### Layer Rules (Enforced by ArchUnit)

`ArchitectureTests` (in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/architecture/`) lets only application
access api and data and only api, data, and application access domain, and it requires cycle-free packages. Further
rules forbid api and application classes to depend on the data ports or on `domain/.../implementation/`, and api
classes to depend on the internal ports or the fixtures. The port packages may hold only interfaces named `*Service`
(`*DataService` in `ports/data/`), and no class in the controller package may be a plain `@Controller`.

### Ports and Adapters

The ports are in `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/`:

- API ports (`ports/api/`), which the controllers call: `UserService` with `getAll`, `getById`, and `create`, and
  `CoffeeConsumptionService` with `getByUserId` and `applyDelta`.
- Data ports (`ports/data/`), which the data module implements: `CrudDataService<DOMAIN, ID>`, `UserDataService` with
  `getByLoginName`, and `CoffeeConsumptionDataService` with `getByUserId`.
- Internal ports (`ports/internal/`), for the fixture loader and the test setup: `DataResetService` with `clearAll`.

The implementations of the API and internal ports are in `domain/.../implementation/`. `UserServiceImpl.create`
checks the login name and the email address against the constants on `User`, trims and lower-cases the address, and
stores the user and then its consumption at zero in one transaction. The fixture loader and the test setup create
the fixture users through `UserService.create`. `CoffeeConsumptionServiceImpl.applyDelta` accepts only `+1`.

### Generic Base Classes

`CrudDataServiceImpl` is the base of the JPA adapters. Its `upsert` gives a domain object with a null id a random
`UUID` and inserts it, and otherwise loads the stored entity (or throws `NotFoundException`) and updates it through the
mapper. `upsert` saves with `saveAndFlush` and turns a unique-constraint violation into the `DuplicationException` of
the matching `ConstraintMapping.REGISTRY` entry. `DtoMapper` and `EntityMapper` are the generic MapStruct interfaces,
and their subtypes put the domain type first (`UserDtoMapper : DtoMapper<User, UserDto>`).

### Naming and File Conventions

- A cross-module interface is a port named `*Service` (`*DataService` for a data port) and implemented by
  `<Port>Impl`, as in `UserService` and `UserServiceImpl`. An interface used within one module is named freely.
- An implementation name never names a library or vendor (`UserDataServiceImpl`, never `JpaUserDataService`). When a
  port has several implementations, name each by its behavior or role.
- A concrete class is not a port and stays out of `ports/`. Stateless helpers without injected dependencies are
  `*Util`, and database entities are `*Entity`.
- Each file holds one top-level type, with related top-level functions and constants beside it (`ErrorResponse.kt`
  also holds `errorCodeFor`). The only multi-type exception is a tightly bound sealed hierarchy.
- Depend on the port, never on the implementation. The ArchUnit test `production code depends on ports, never on Impl
  types` enforces this rule for every class except the `*Impl` and `@Configuration` classes. It exempts the `*Impl`
  classes because each `*DataServiceImpl` extends `CrudDataServiceImpl`.

## Build and Run Commands

Run Gradle in a shell with mise activated or as `mise exec -- gradle ...`, because Java 25 and Gradle 9.5 come from
`mise.toml` and there is no Gradle wrapper. The `java` entry in `gradle/libs.versions.toml` sets the toolchain and the
Kotlin `jvmTarget`, and `mise.toml` pins the same major version by hand. The dev database and the Testcontainers tests
need Docker.

```shell
gradle build                                                 # compile, ktlint, detekt, and every test
gradle ktlintFormat                                          # apply the formatting fixes before a commit
gradle :domain:test --tests "CoffeeConsumptionServiceTest"   # one test class
gradle :application:test --tests "*ArchitectureTests"        # the layer rules, which need no Docker
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

Scope a `--tests` filter to one module, because the filter fails every module without a matching test. Quote the
filter, since test methods use backtick names. Without Docker, `gradle build -x :application:test -x :data:test` runs
the rest of the build.

Continuous integration (CI), in `.github/workflows/build.yml`, runs `scripts/check-version-sync.sh` and `gradle build`
on every push to `main` and on every pull request. After each push to `main`,
`.github/workflows/dependency-submission.yml` submits the application's runtime dependencies to GitHub's dependency
graph, which Dependabot alerts and Dependabot security updates read. GitHub cannot read a Gradle build on its own. Pin
every workflow `uses:` to a full commit SHA with a trailing `# vX.Y.Z` comment.

### Format, Lint, and Static Analysis

The `check` task runs ktlint and detekt, so `gradle build` fails on any finding. The ktlint check applies the
`ktlint_official` code style with the settings of the root `.editorconfig`, and detekt applies
`config/detekt/detekt.yml` on top of its own defaults. Fix each finding rather than adding a detekt baseline. Bump
Kotlin and detekt together, by hand, because detekt `2.0.0-alpha.6` runs only with Kotlin 2.4.10, the version that it
was built against. A lone Kotlin bump fails every detekt task, and a Dependabot rule ignores the Kotlin coordinates for
that reason.

The KDoc rule set `campus-coffee-kdoc`, in `detekt-rules/src/main/kotlin/de/seuhd/campuscoffee/detekt/` and tested by
`gradle :detekt-rules:test`, requires KDoc on every non-local class, interface, object, and enum class and on every
non-local, non-override function in `src/main`. The rules also cover private nested classes and `@Bean` methods. A
public, non-override function needs an `@param` for every parameter, and a non-private enum constructor property needs
an `@property` or `@param`. Test sources are exempt.

## Development Workflow

Substantial changes follow a plan, review, implement, review, release loop:

1. Explore the code and write a concrete plan with the files to touch, the approach, and the verification. Resolve
   open decisions with the requester first.
2. Review the plan adversarially against the actual code, looking for wrong assumptions, missed edge cases, and
   regressions, and verify each finding in the source. Revise the plan with the confirmed findings.
3. Implement the plan in coherent steps, keep `gradle build` green, and reuse existing patterns and helpers.
4. Review the diff adversarially for bugs that the green build does not catch, such as untested edge cases. Verify
   each finding against the code, apply the confirmed fixes, and run the build again.
5. Release the change (see Versioning and Releases).

Scale the review depth to the change. A small fix needs a light review, and a feature needs a thorough review. A
decision worth recording goes into `doc/adr/` as an architecture decision record (ADR) named `NNN-meaningful-name.md`,
following `doc/adr/template.md`, and kept short. An accepted ADR stays as written, and a new ADR supersedes it.

## Versioning and Releases

The project follows [Semantic Versioning](https://semver.org/) and keeps a
[Keep a Changelog](https://keepachangelog.com/)-style `CHANGELOG.md`. Every notable change adds a bullet under
`## [Unreleased]` in its group (e.g., `Added`, `Changed`, `Fixed`). A release moves the bullets under a new
`## [x.y.z] - YYYY-MM-DD` header, with a patch bump for tooling and internal cleanups, a minor bump for features, and
a major bump for breaking changes. `version` in the root `gradle.properties` must equal the most recent `## [x.y.z]`
header, which `scripts/check-version-sync.sh` checks in CI. Before a release, resolve what `gh issue list` and
`gh pr list` show. Tag the release commit with `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z`, and add the
link reference `[x.y.z]: https://github.com/se-uhd/coffee-kitty/releases/tag/vX.Y.Z` at the bottom of `CHANGELOG.md`.

## Commit Messages

A commit message must stand on its own for a reader of `git log` who has no access to your notes, with a concise
imperative subject line and a body that explains the reason for the change and any non-obvious decisions. Never cite
references that are not in the repository, such as the ids of review findings or ticket shorthands. Group related work
into a few cohesive commits. End every commit message that Claude Code writes with the `Co-Authored-By:` trailer from
the harness instructions.

## Database

The database is PostgreSQL 18, with Flyway migrations in `data/src/main/resources/db/migration/` and JPA with Spring
Data. `V1__create_users_table.sql` creates `users` with the unique constraints `uq_users_login_name` and
`uq_users_email_address`. `V2__create_coffee_consumptions_table.sql` creates `coffee_consumptions` with
`uq_coffee_consumptions_user` and `ON DELETE CASCADE` on its foreign key to `users`. Hibernate validates the schema
and never changes it (`ddl-auto: validate`), so every entity must match its table exactly.

- Keep the migrations lean, with plain DDL (data definition language) and no comments. The rationale for a column or a
  constraint belongs in the KDoc of its entity class.
- Never edit an applied migration. Flyway compares the checksum of every applied migration at startup, so any change
  to an applied file, even to a comment, stops the application from starting. Add a new migration instead.
- Keep each migration compatible with the previous release, because in a rolling deployment the previous release keeps
  serving while the next release migrates the schema. A renamed or dropped column, or a NOT NULL column without a
  default, then ships in two releases (add and backfill first, remove or tighten later).

## Testing Strategy

- Unit tests: `UserServiceTest` and `CoffeeConsumptionServiceTest` in `domain/src/test/kotlin/`, with Mockito mocks.
- Fixture loader test: `FixtureStartupLoaderTest` in `application/src/test/kotlin/` checks that the loader seeds a
  database without users, leaves a database with users unchanged, and clears the data first under `reset-on-startup`.
- Integration test: `UniqueConstraintDuplicationIntegrationTest` in `data/src/test/kotlin/`, on
  `AbstractDataIntegrationTest` and `DataTestApplication`, against PostgreSQL in Testcontainers.
- System tests: `UserSystemTests` and `ConsumptionSystemTests` in
  `application/src/test/kotlin/de/seuhd/campuscoffee/tests/system/`, extending `AbstractSystemTest`, with
  Testcontainers and Spring's `RestTestClient`.
- Acceptance tests: Cucumber scenarios for behavior-driven development (BDD) in
  `application/src/test/kotlin/de/seuhd/campuscoffee/tests/acceptance/`, with `consumption.feature` in
  `application/src/test/resources/de/seuhd/campuscoffee/tests/acceptance/`.
- Architecture tests: `ArchitectureTests` (see Layer Rules).

### Test Naming

Test methods (annotated with `@Test` or `@ParameterizedTest`) use Kotlin backtick names that read as a sentence in
active voice and present tense, with the subject under test first and then the outcome stated as the fact that the
test asserts (e.g., an HTTP status, an exception type, or a returned value). Avoid `should`. Examples are
``fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`()`` and
``fun `applyDelta with a delta other than +1 throws ValidationException and writes nothing`()``. Other functions
keep camelCase names.

## Key Technologies

- Spring Boot 4 (Spring Framework 7) and Kotlin 2.4 on Java 25.
- Spring Data JPA with Hibernate, PostgreSQL 18, and Flyway.
- MapStruct via kapt (the Kotlin annotation processing tool), Bean Validation in the controllers, and SpringDoc for
  the OpenAPI definition and Swagger UI.
- JUnit 6, Mockito with mockito-kotlin, AssertJ, Testcontainers, Cucumber, and ArchUnit for the tests.
- ktlint for the formatting, and detekt with the `campus-coffee-kdoc` rule set for the static analysis.

## REST API Endpoints

The endpoints accept and return JSON only and are all open. `ApiWebConfig` adds the `/api` prefix, and controllers map
paths relative to the resource.

- `GET /users`: all users, ordered by login name.
- `POST /users`: create a user and its consumption at zero (201 Created with a `Location` header). An `id` in the body
  is a 400 `ValidationException`, and a login name or email address that is taken is a 409 `DuplicationException`.
- `GET /users/{id}`: one user, or a 404 `NotFoundException`.
- `GET /users/{userId}/consumption`: the user's `ConsumptionDto` with `loginName` and `count`.
- `POST /users/{userId}/consumption` with `{ "delta": 1 }`: add one coffee. Another delta is a 400
  `ValidationException` from the domain, and a missing delta is a 400 `BadRequest`.

Both consumption endpoints return a 404 `NotFoundException` for an unknown user.

## Configuration

`application/src/main/resources/application.yaml` has a default block and a `dev` block. The only custom keys are the
fixture flags of `FixturesProperties`, both on in dev. `campus-coffee.fixtures.load-on-startup` registers
`FixtureStartupLoader`, which seeds the fixture users into a database without users, and
`campus-coffee.fixtures.reset-on-startup` makes the loader clear the data first. The `dev` block also sets the
datasource on `localhost:${DB_PORT:5433}`, the port `${SERVER_PORT:8081}`, and the springdoc paths, so the API is at
`http://localhost:8081/api` and Swagger UI at `http://localhost:8081/api/swagger-ui.html`. Outside dev, springdoc is
off.

## Important Patterns

- Error handling: The domain throws `NotFoundException` (404), `DuplicationException` (409), and
  `ValidationException` (400). `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler`, which maps Spring's
  standard web exceptions to their status codes (e.g., 404 for an unknown path). Any other exception is a 500. Every
  error body is an `ErrorResponse`. Its `errorCode` is the class name of the domain exception or, for any other error,
  the reason phrase of the status without spaces (e.g., `BadRequest`).
- MapStruct: The `kotlin-kapt-conventions` plugin runs MapStruct through kapt with the option
  `mapstruct.unmappedTargetPolicy=ERROR`, which fails the build on a target property with neither a source nor
  `ignore = true`.
- IDE configuration metadata: Every custom key has a `*Properties` class with KDoc, from which the IDE resolves the key
  in `application.yaml`. `application` depends on `data` at compile scope, not `runtimeOnly`, so that the IDE also
  resolves the keys of a `*Properties` class in the data module.
- Ids and timestamps: The `upsert` method assigns a random `UUID`, and `PersistableEntity` implements
  `Persistable<UUID>`, which lets the repository insert a new entity without a preceding SELECT. `Entity` sets the
  timestamps in `@PrePersist` and `@PreUpdate`, truncated to the microsecond precision of PostgreSQL, so a timestamp
  that `upsert` returns equals the one that a later query reads.

## Adding a New Entity

1. Create the domain model in `domain/.../model/` (implement `DomainModel<UUID>`).
2. Create the service interface in `domain/.../ports/api/`. An operation that only the fixture loader or the test
   setup calls goes on an internal port in `domain/.../ports/internal/`.
3. Create the data service interface in `domain/.../ports/data/` (extend `CrudDataService<DOMAIN, ID>`).
4. Create the service implementation in `domain/.../implementation/`.
5. Create the JPA entity in `data/.../persistence/entities/` (extend `Entity`).
6. Create the repository in `data/.../persistence/repositories/` (extend `JpaRepository`).
7. Create the entity mapper in `data/.../mapper/` (extend `EntityMapper`).
8. Create the data service implementation in `data/.../implementations/` (extend `CrudDataServiceImpl`).
9. Add a `ConstraintMapping.REGISTRY` entry for each unique constraint.
10. Clear the new entity's data in `DataResetServiceImpl.clearAll`, before the data that the entity references.
11. Create the DTO in `api/.../dtos/` (implement `Dto<UUID>`) and the DTO mapper in `api/.../mapper/`.
12. Create the controller in `api/.../controller/`.
13. Create a Flyway migration in `data/src/main/resources/db/migration/`.
