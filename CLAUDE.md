# CLAUDE.md

## Project Overview

Coffee Kitty is a minimal coffee counter with the architecture of the coffee app of SE@UHD (the Software Engineering
Group at Heidelberg University, hence the `de.seuhd` package). A software engineering lecture uses it as its running
example, and students extend it over the semester. A user has a login name, an email address, and a first and a last
name, and each user has one consumption with a coffee count that starts at zero. The REST API lists, creates, and reads
users, reads a coffee count, and adds one coffee at a time. Every endpoint is open, because Coffee Kitty has no
authentication. There is no frontend, and Swagger UI in the dev profile documents the API. `GLOSSARY.md` defines the
vocabulary. Use its terms in code, messages, and docs.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters) architecture.
The included build `build-logic/` holds the convention plugins, which the subprojects apply by the id
`de.seuhd.campuscoffee.<name>`, and `gradle/libs.versions.toml` is the version catalog of the root build and of
`build-logic/`. `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md` records why the build has this
structure. The `detekt-rules` subproject holds the custom detekt rules. `settings.gradle.kts` and
`build-logic/settings.gradle.kts` declare every Maven repository of the build, and `FAIL_ON_PROJECT_REPOS` fails the
build when a build script declares its own.

### Module Dependencies

- `domain`: the models, the port interfaces, and the business logic. It depends on the Spring Boot core, `spring-tx`,
  and kotlin-logging, and never on Bean Validation, Jakarta Persistence (JPA), or the api, data, or application
  modules.
- `api`: the controllers, the data transfer objects (DTOs), the DTO mappers, the exception handler, and the OpenAPI
  definition. Depends on domain.
- `data`: the JPA entities, the repositories, the entity mappers, the data adapters, and the Flyway migrations. Depends
  on domain.
- `application`: the composition root, the module that wires the other modules into one Spring Boot application. It
  holds the main class, the fixture loader with its configuration properties, and `application.yaml`, and no web or
  business code. Depends on domain, api, and data.

### Layer Rules (Enforced by ArchUnit)

`ArchitectureTests` (in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/architecture/`) allows only application
to access api and data, and only api, data, and application to access domain. It also requires the production packages
to be free of cycles. Every class, test classes included, must lie in a layer: `api..`, `domain..`, `data..`, or the
application packages, which are the root package `de.seuhd.campuscoffee`, `configuration..`, and `tests..`. A class in a
new package of the application module fails the layer test until the package is added to
`APPLICATION_PRODUCTION_PACKAGES`. Further rules forbid the production classes of api and application to depend on the
data ports or on `domain/.../implementation/`, and api classes to depend on the internal ports or on `TestFixtures` in
`domain/.../tests/`. The port packages may hold only interfaces named `*Service` (`*DataService` in `ports/data/`), and
no class in the controller package may be a plain `@Controller`.

### Ports and Adapters

The ports are in `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/`:

- API ports (`ports/api/`), which the controllers call, such as `UserService`.
- Data ports (`ports/data/`), which the data module implements: the generic `CrudDataService<DOMAIN, ID>` and, for each
  entity, a `*DataService` that extends it, such as `UserDataService`.
- Internal ports (`ports/internal/`), for the fixture loader and the test setup, such as `DataResetService`.

The implementations of the API and internal ports are in `domain/.../implementation/`. `UserServiceImpl.create`
normalizes the email address, checks the new user against the limits on `User`, and stores the user and then its
consumption at zero in one transaction. The fixture loader and the test setup create the fixture users through
`UserService.create`. `CoffeeConsumptionServiceImpl.applyDelta` accepts only `+1`. It reads the consumption with
`getByUserIdForUpdate`, which locks the row until the transaction ends. A second request that adds a coffee for the same
user at the same time then waits, so that both coffees count.

### Generic Base Classes

`CrudDataServiceImpl` is the base of the data adapters. Its `upsert` inserts a domain object with a null id under a
random `UUID`, updates the stored JPA entity of an object with an id through the entity mapper (or throws
`NotFoundException`), and turns a violated unique constraint into the `DuplicationException` of the matching
`ConstraintMapping.REGISTRY` entry. Except for `getByUserIdForUpdate`, the read methods of `CrudDataServiceImpl` and of
the adapters are not `@Transactional`, so for a caller without a transaction they map the JPA entity outside one. An
adapter for a JPA entity with a lazy association must therefore read and map it inside a `@Transactional` method.
`DtoMapper` and `EntityMapper` are the generic MapStruct interfaces, and their subtypes put the domain type first
(`UserDtoMapper : DtoMapper<User, UserDto>`). An entity mapper overrides `updateEntity` to ignore the id and the
timestamps, as `UserEntityMapper` does.

### Naming and File Conventions

- A cross-module interface is a port named `*Service` (`*DataService` for a data port) and implemented by
  `<Port>Impl`, as in `UserService` and `UserServiceImpl`. An interface used within one module is named freely.
- An implementation name never names a library or vendor (`UserDataServiceImpl`, never `JpaUserDataService`). When a
  port has several implementations, name each by its behavior or role.
- A concrete class is not a port and stays out of `ports/`. Stateless helpers without injected dependencies are
  `*Util`, and JPA entities are `*Entity`.
- Each file holds one top-level type, with related top-level functions and constants beside it (`ErrorResponse.kt`
  also holds `errorCodeFor`). The only multi-type exception is a tightly bound sealed hierarchy.
- Depend on the port, never on the implementation. The ArchUnit test
  `production code depends on ports, never on Impl types` enforces this rule for every production class except the
  `@Configuration` classes, so one `*Impl` class may not depend on another either. The rule checks only dependencies
  on concrete `*Impl` classes, which lets each `*DataServiceImpl` extend the abstract base `CrudDataServiceImpl`.

## Build and Run Commands

Run Gradle in a shell with mise activated or as `mise exec -- gradle ...`, because mise installs Java and Gradle from
`mise.toml` and there is no Gradle wrapper. The `java` entry in `gradle/libs.versions.toml` sets the toolchain and the
Kotlin `jvmTarget`, and `mise.toml` pins the same major version by hand. The dev database and the Testcontainers tests
need Docker.

```shell
gradle build                                                 # compile, ktlint, detekt, and every test
gradle ktlintFormat                                          # apply the formatting fixes before a commit
gradle :domain:test --tests "CoffeeConsumptionServiceTest"   # one test class
gradle :application:test --tests "ArchitectureTests"         # the layer rules only, which need no Docker
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

Scope a `--tests` filter to one subproject, because the filter fails every subproject without a matching test. Quote the
filter, since test methods use backtick names. In `:application:test`, a filter that starts with `*` also runs the
Cucumber scenarios, which need Docker. Without Docker, `gradle build -x :application:test -x :data:test` runs the rest
of the build.

The Gradle configuration cache is on (`gradle.properties`). A task action must not use a value of its build script
(copy the value into a local `val` first) or a `Configuration` (wrap it in `files(...)`), or the build fails with a
configuration cache problem.

Continuous integration (CI), in `.github/workflows/build.yml`, runs `scripts/check-version-sync.sh` and `gradle build`
on every push to `main` and on every pull request. After each push to `main`,
`.github/workflows/dependency-submission.yml` submits the runtime dependencies of the application to GitHub's dependency
graph, which Dependabot alerts and security updates read. Pin every workflow `uses:` to a full commit SHA with a
trailing `# vX.Y.Z` comment.

### Format, Lint, and Static Analysis

The `check` task runs ktlint and detekt, so `gradle build` fails on any finding. ktlint applies the `ktlint_official`
code style with the settings of the root `.editorconfig`, and detekt applies `config/detekt/detekt.yml` on top of its
own defaults. Fix each finding rather than adding a detekt baseline. detekt and ktlint run on their own Kotlin
compilers, the catalog's `detekt-kotlin` and `ktlint-kotlin`, independent of the project's `kotlin`. When you bump
detekt, set `detekt-kotlin` to the compiler that the release names (detekt reports a mismatch as "detekt was compiled
with Kotlin X").

The KDoc rule set `campus-coffee-kdoc`, in `detekt-rules/src/main/kotlin/de/seuhd/campuscoffee/detekt/` and tested by
`gradle :detekt-rules:test`, requires KDoc on every non-local class, interface, object, and enum class and on every
non-local, non-override function. A public, non-override function needs an `@param` for every parameter, and a
non-private enum constructor property needs an `@property` or `@param`. detekt checks only `src/main`, so no detekt
rule checks the test sources.

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

Scale the review depth to the change, from a light review for a small fix to a thorough review for a feature. A
decision worth recording goes into `doc/adr/` as an architecture decision record (ADR) named `NNN-meaningful-name.md`,
following `doc/adr/template.md`, and kept short. An accepted ADR is not edited, except for its status when a later ADR
supersedes it, so a changed decision goes into a new ADR that refers to it.

## Versioning and Releases

The project follows [Semantic Versioning](https://semver.org/) and keeps a
[Keep a Changelog](https://keepachangelog.com/)-style `CHANGELOG.md`. Every notable change adds a bullet under
`## [Unreleased]` in its group (e.g., `Added`, `Changed`, `Fixed`). A release takes a patch bump for bug fixes,
tooling, and internal cleanups, a minor bump for features, and a major bump for breaking changes. `version` in the root
`gradle.properties` must equal the most recent `## [x.y.z]` header, which `scripts/check-version-sync.sh` checks in
CI. To release a version:

1. Fix, merge, or close with a reason every open issue and pull request that `gh issue list` and `gh pr list` show.
2. Move the bullets of `## [Unreleased]` under a new `## [x.y.z] - YYYY-MM-DD` header, add the link reference
   `[x.y.z]: https://github.com/se-uhd/coffee-kitty/releases/tag/vX.Y.Z` at the bottom of `CHANGELOG.md`, and set
   `version` in `gradle.properties` to the new version.
3. Commit these changes as the release commit and push it to `main`.
4. When CI passes on that commit, tag it and push the tag with
   `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z`.

## Commit Messages

A commit message must stand on its own for a reader of `git log` who has no access to your notes, with a concise
imperative subject line and a body that explains the reason for the change and any non-obvious decisions. Never cite
references that are not in the repository, such as the ids of review findings or ticket shorthands. Group related work
into a few cohesive commits. End every commit message that Claude Code writes with the `Co-Authored-By:` trailer from
the harness instructions.

## Database

The database is PostgreSQL, with Flyway migrations in `data/src/main/resources/db/migration/` and JPA with Spring Data.
Hibernate checks at startup (`ddl-auto: validate`) that every mapped table and column exists with a compatible type,
and it never changes the schema. The check ignores the NOT NULL, CHECK, and unique constraints and any column that no
JPA entity maps, so an INSERT or UPDATE that violates one of them, such as a null in a NOT NULL column, fails only when
it runs, not at startup.

- Keep the migrations lean, with plain DDL (data definition language) and no comments. The rationale for a column or a
  constraint belongs in the KDoc of its entity class.
- Never edit a migration that others have applied, such as one that is pushed or released. Flyway compares the
  checksum of every applied migration at startup, so any change to an applied file, even to a comment, stops the
  application from starting. Add a new migration instead. You may still edit a new migration that only your own dev
  database has applied, but then remove that database with `docker rm -f -v campus-coffee-db` and start a new container
  with the `docker run` command above.
- Once the app runs in a rolling deployment, which replaces the running instances one at a time, keep each migration
  compatible with the previous release, because the previous release keeps serving while the next release migrates
  the schema. A renamed or dropped column, or a NOT NULL column without a default, then ships in two releases (add and
  backfill first, remove or tighten later).

## Testing Strategy

- Unit tests: domain service tests such as `UserServiceTest` in `domain/src/test/kotlin/`, and
  `FixtureStartupLoaderTest` in `application/src/test/kotlin/`, with Mockito mocks.
- Integration tests: `CrudDataServiceIntegrationTest` and `ConstraintMappingRegistryIntegrationTest` in
  `data/src/test/kotlin/`, on `AbstractDataIntegrationTest` and `DataTestApplication`, against PostgreSQL in
  Testcontainers. `ConstraintMappingRegistryIntegrationTest` fails when a unique constraint or unique index has no
  `ConstraintMapping.REGISTRY` entry.
- System tests: classes such as `UserSystemTests` in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/system/`,
  extending `AbstractSystemTest`, with Spring's `RestTestClient` and the PostgreSQL container from Testcontainers that
  `SystemTestUtils` shares with the acceptance tests.
- Acceptance tests: Cucumber scenarios for behavior-driven development (BDD) in
  `application/src/test/kotlin/de/seuhd/campuscoffee/tests/acceptance/`, with the feature files, such as
  `consumption.feature`, in `application/src/test/resources/de/seuhd/campuscoffee/tests/acceptance/`.
- Architecture tests: `ArchitectureTests` (see Layer Rules).

### Test Naming

Test methods (annotated with `@Test` or `@ParameterizedTest`) use Kotlin backtick names that read as a sentence in
active voice and present tense, with the subject under test first and then the outcome stated as the fact that the
test asserts (e.g., an HTTP status, an exception type, or a returned value). Avoid `should`. Examples are
``fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`()`` and
``fun `applyDelta with a delta other than +1 throws ValidationException and writes nothing`()``. Other functions
keep camelCase names.

## Key Technologies

- Spring Boot and Kotlin on Java, with the versions in `gradle/libs.versions.toml` and `mise.toml`.
- Spring Data JPA with Hibernate, PostgreSQL, and Flyway.
- MapStruct via kapt (the Kotlin annotation processing tool), Bean Validation in the controllers, and springdoc for
  the OpenAPI definition and Swagger UI.
- JUnit, Mockito with mockito-kotlin, AssertJ, Testcontainers, Cucumber, and ArchUnit for the tests.
- ktlint for the formatting, and detekt with the `campus-coffee-kdoc` rule set for the static analysis.

## REST API Endpoints

The endpoints accept and return JSON only and are all open. `ApiWebConfig` adds the `/api` prefix, and controllers map
paths relative to the resource. The REST API section of `README.md` lists every endpoint with its status codes and a
runnable curl command, and Swagger UI in the dev profile shows the full contract.

## Configuration

`application/src/main/resources/application.yaml` has a default block and a `dev` block. `FixturesProperties` binds
the fixture flags, both on in dev. `campus-coffee.fixtures.load-on-startup` registers `FixtureStartupLoader`, which
seeds the fixture users into a database without users, and `campus-coffee.fixtures.reset-on-startup` makes the loader
clear the data first. The `dev` block also sets the datasource on `localhost:${DB_PORT:5433}`, the port
`${SERVER_PORT:8081}`, and the springdoc paths, so the API is at `http://localhost:8081/api` and Swagger UI at
`http://localhost:8081/api/swagger-ui.html`. Outside dev, springdoc is off.

## Important Patterns

- Error handling: The domain defines `NotFoundException` (404), `DuplicationException` (409), and
  `ValidationException` (400). The data adapters throw the first two (see Generic Base Classes), and the domain
  services and `UserController` throw `ValidationException`. `GlobalExceptionHandler` extends
  `ResponseEntityExceptionHandler`, which maps Spring's standard web exceptions to their status codes (e.g., 404 for
  an unknown path). Any other exception is a 500. Every error body is an `ErrorResponse`. Its `errorCode` is the class
  name of the domain exception or, for any other error, the reason phrase of the status without spaces (e.g.,
  `BadRequest`). A new domain exception needs the entries that the KDoc of
  `GlobalExceptionHandler.handleMappedException` lists.
- MapStruct: The `kotlin-kapt-conventions` plugin runs MapStruct through kapt with the option
  `mapstruct.unmappedTargetPolicy=ERROR`, which fails the build on a target property with neither a source nor
  `ignore = true`.
- IDE configuration metadata: Every custom key has a `*Properties` class with KDoc, from which the IDE resolves the key
  in `application.yaml`. `application` depends on `data` at compile scope, not `runtimeOnly`, so that the IDE also
  resolves the keys of any `*Properties` class in the data module.
- Ids and timestamps: The `upsert` method assigns a random `UUID`, and `PersistableEntity` implements
  `Persistable<UUID>`, which lets the repository insert a new JPA entity without a preceding SELECT. `Entity` sets the
  timestamps in `@PrePersist` and `@PreUpdate`, truncated to the microsecond precision of PostgreSQL, so a timestamp
  that `upsert` returns equals the one that a later query reads.
- Time: Every timestamp is in UTC, in the database, in the code, and in the API, which serves it without an offset.
  Read the clock only with `LocalDateTime.now(ZoneOffset.UTC)`, never in the system time zone. A client converts a
  timestamp to a local time zone only when it displays it.

## Adding a New Entity

The order keeps `gradle build` green after each step. A port without an implementation breaks nothing, so the ports
come first. The implementation of the API port (step 8) comes after the data adapter (step 7), because a `@Service`
needs a bean for each port that it uses.

1. Create the domain model in `domain/.../model/` (implement `DomainModel<UUID>` with a nullable `id`, `createdAt`,
   and `updatedAt`, as in `User`).
2. Create the API port in `domain/.../ports/api/`. An operation that only the fixture loader or the test setup calls
   goes on an internal port in `domain/.../ports/internal/`.
3. Create the data port in `domain/.../ports/data/` (extend `CrudDataService<DOMAIN, UUID>`).
4. In one step, create the Flyway migration in `data/src/main/resources/db/migration/`, the JPA entity in
   `data/.../persistence/entities/` (extend `Entity`, and write the annotation fully qualified as
   `@jakarta.persistence.Entity`), and a `ConstraintMapping.REGISTRY` entry for each unique constraint. Hibernate
   checks every JPA entity against the schema at startup, and `ConstraintMappingRegistryIntegrationTest` fails while a
   unique constraint and its registry entry are not both there. Choose the `ON DELETE` action of each foreign key:
   `CASCADE` only for a row that belongs to the referenced row, as a consumption belongs to its user. Under the
   default `NO ACTION`, deleting a referenced row fails with a `DataIntegrityViolationException`, which the client
   gets as a 500, so an operation that deletes a row checks for references first and throws a new domain exception
   that `GlobalExceptionHandler` maps to 409 Conflict.
5. Create the repository in `data/.../persistence/repositories/` (extend `JpaRepository`).
6. Create the entity mapper in `data/.../mapper/` (extend `EntityMapper` and override `updateEntity` to ignore the id
   and the timestamps, as `UserEntityMapper` does). Map each reference to another entity by its id, as
   `CoffeeConsumptionEntityMapper` does: add a lookup such as `userReference` to `EntityReferenceResolver`, list the
   resolver and the referenced mapper in `@Mapper(uses = ...)`, and put
   `@Mapping(target = "<reference>", source = "<reference>.id")` on `toEntity` and `updateEntity`. Without these
   mappings, MapStruct maps the nested domain object (such as the `User` in a `CoffeeConsumption`) through the
   referenced mapper, and an update writes the fields of that nested object over the referenced row.
7. Create the data adapter in `data/.../implementations/` (extend `CrudDataServiceImpl`).
8. Create the implementation of the API port in `domain/.../implementation/`.
9. Clear the new entity's data in `DataResetServiceImpl.clearAll`, after the data of every entity that references it
   and before the data of every entity that it references.
10. Create the DTO in `api/.../dtos/` (implement `Dto<UUID>` with a nullable `id`, `createdAt`, and `updatedAt`,
    marked read-only in the OpenAPI definition, as in `UserDto`) and the DTO mapper in `api/.../mapper/`.
11. Create the controller in `api/.../controller/`. As in `UserController.create`, an endpoint that creates an object
    rejects a body with an `id` and answers 201 Created with a `Location` header.
12. Add tests modeled on the existing ones: a domain unit test as in `UserServiceTest`, cases in
    `CrudDataServiceIntegrationTest` (including an update without timestamps, which fails when the entity mapper copies
    them), and a system test class on `AbstractSystemTest`.
13. Update Ports and Adapters in this file for the new ports, list the new endpoints in the REST API section of
    `README.md` with one runnable curl block per endpoint, add the new terms to `GLOSSARY.md`, and update the texts
    that summarize the entities, such as the README introduction, the description in `OpenApiConfig`, and the
    glossary entries "data reset" and "domain exception".
