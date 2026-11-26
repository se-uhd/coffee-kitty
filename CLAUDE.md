# CLAUDE.md

## Project

Coffee Kitty is a minimal coffee counter and the running example of a software engineering lecture of SE@UHD, the
Software Engineering Group at Heidelberg University. Students extend it over the semester. Coffee Kitty has the
architecture of the coffee app of SE@UHD, Campus Coffee, so the packages start with `de.seuhd.campuscoffee`, and the
plugin ids and configuration keys contain `campuscoffee` or `campus-coffee`. `README.md` describes the features and the
setup, and Swagger UI the REST API.

The app is a Spring Boot app in Kotlin with PostgreSQL, Spring Data JPA, and Flyway. It implements FR-3, FR-8, FR-9,
FR-13, and FR-14 of `requirements/`. The other requirements specify money and ratings, and the data module already
holds their tables. Every user has the role `USER` or `ADMIN`. The app has no authentication, so the header
`X-Acting-User` names the acting user.

`GLOSSARY.md` defines one term for each concept. Use these terms in code, messages, and documentation.

## Hard constraints

- The tests of the undo status (FR-9) are the reference solution of assignment 7, which students submit by 2026-12-08.
  Never commit these tests to a pushed branch before that date. On that date, add the tests and delete the exemptions
  of the undo status in `coverage/build.gradle.kts` and `pitest-conventions.gradle.kts`. On the same date, add the undo
  status to `models/src/main/tla/Consumption.tla` and replay `undoableUntil` in `ConsumptionConformanceTest`.
- Never edit an accepted ADR except for its status. A correction of the wording that does not change the decision is
  allowed, and ADR-010 takes new measurements. Record a changed decision in a new ADR that refers to the old one, and
  set the status of the old ADR to superseded, or to "Accepted, amended by [ADR-NNN](NNN-name.md)" when the new ADR
  changes only a part of it (ADR 017).
- Every threshold of the build, such as the coverage minimums, the mutation score, the duplicate size, the memory of the
  Gradle daemon, and the wait times of the tests, has a measurement on Coffee Kitty. Before you change a threshold or
  add one, measure, record the result in `doc/adr/010-thresholds-of-the-build.md`, and write a new ADR. Do not copy a
  threshold from another project.
- Fix each finding of ktlint and detekt, and do not add a detekt baseline.

## Modules

The build has seven subprojects. The four modules hold the app in a hexagonal (ports-and-adapters) architecture:

- `domain`: the domain models, the ports, and the domain services. It depends on the Spring Boot core, `spring-tx`, and
  the API of Jakarta Bean Validation, never on Spring MVC, JPA, or another module.
- `api`: the controllers, the DTOs and their mappers, the exception handler, and the OpenAPI definition. Depends on
  `domain`.
- `data`: the JPA entities, the repositories, the entity mappers, the data adapters, and the Flyway migrations. Depends
  on `domain`.
- `application`: the composition root with the main class, the startup task, and `application.yaml`, but no web or
  business code. Its test sources hold the system and acceptance tests and the dev application. Depends on `domain`,
  `api`, and `data`.

`coverage` creates the coverage report of all modules, `detekt-rules` holds the custom detekt rules, and `models`
checks TLA+ and Alloy models against the code. The included build `build-logic/` holds the convention plugins.

`ArchitectureTests` in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/architecture/` checks the layer rules:

- Only `application` may access `api` and `data`. The production code of `api` and `application` must not depend on
  the data ports or on `domain/.../implementation/`, and `api` must not depend on the internal ports.
- `data` may depend only on the models, the exceptions, and the data ports of the domain. The test lists these
  packages, so add a new package of the domain to the test before `data` uses it.
- Code depends on a port, never on a `*Impl` class.
- Add a new production package of `application` to `APPLICATION_PRODUCTION_PACKAGES`.

## Conventions

- Name a port `*Service`, or `*DataService` for a data port, and its implementation `<Port>Impl`, as in `UserService`
  and `UserServiceImpl`. An interface used within one module may have any name.
- Name an implementation after its behavior or role, never after a library or vendor (`UserDataServiceImpl`, not
  `JpaUserDataService`).
- Name a stateless helper without injected dependencies `*Util`, and a JPA entity `*Entity`.
- Put one top-level type in each file, with its related top-level functions and constants. Only a tightly bound sealed
  hierarchy may put several types in one file.
- Store, compute, and serve every timestamp in UTC, and serve it without an offset. A domain service reads the time
  from the injected `Clock`. Other code calls `LocalDateTime.now(ZoneOffset.UTC)`, never the system time zone. The
  tests run in the time zone Asia/Tokyo, so a test fails when code uses the time zone of the JVM.
- Declare every custom configuration key in a `*Properties` class, from which the IDE resolves the key.
- KDoc is required on contracts: the interfaces in a package `ports`, their functions, and the `*Properties` classes.
  Elsewhere, write KDoc only for what the code cannot say. A KDoc has at most 6 lines on a type and 3 elsewhere, and
  one line per tag. Private members have no KDoc. Other comments are only `// ADR-NNN` or
  `// External: <url> <reason>`. `gradle detekt` checks `src/main`, and
  `gradle detektComments` checks the tests and the build scripts.
- `config/markdown/limits.yml` lists every Markdown file with its limits, and `gradle markdownLimits` fails for an
  unlisted file or a broken limit. `CHANGELOG.md` follows Keep a Changelog with bullets of at most 2 lines.

## Commands

Run Gradle as `mise exec -- gradle ...` or in a shell with mise activated. The project has no Gradle wrapper, and mise
installs Java, Gradle, and TLC from `mise.toml`. The dev database and the Testcontainers tests need Docker.

```shell
gradle build                                                 # compile, every check and test, coverage gate
gradle ktlintFormat                                          # fix the formatting before a commit
gradle :domain:test --tests "CoffeeConsumptionServiceTest"   # one test class
gradle :application:test --tests "ArchitectureTests"         # the layer rules, without Docker
gradle :domain:pitest                                        # mutation tests of the domain services
gradle build -x :application:test -x :data:test -x :coverage:coverageGate   # the build without Docker
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
gradle :application:bootTestRun                              # the dev application with the user fixtures
```

Scope a `--tests` filter to one subproject, because Gradle fails each subproject without a matching test. Quote the
filter, because test names contain spaces. In `:application:test`, start the filter with a class name or a package,
never with `*`, or the suite `CucumberTests` fails with `initializationError`.

## Workflow

Scale the depth of each review to the change. A substantial change goes through these steps:

1. Explore the code and write a plan with the files to touch, the approach, and how to verify the change. Resolve open
   decisions with the requester before you go on.
2. Review the plan adversarially against the code. Look for wrong assumptions, missed edge cases, and regressions,
   verify each finding in the source, and revise the plan.
3. Implement the plan in coherent steps, reuse existing patterns and helpers, and check that `gradle build` passes after
   each step.
4. Review the diff adversarially for bugs that pass `gradle build`, such as untested edge cases. Verify each finding,
   apply the confirmed fixes, and run the build again. After a change to the domain module, also run
   `gradle :domain:pitest`.
5. Release the change.

Record each decision worth recording as a short ADR in `doc/adr/`, named `NNN-meaningful-name.md` and based on
`doc/adr/template.md`. Refer to it in code with `// ADR-NNN`.

## Releases

The project follows Semantic Versioning. For every notable change, add a bullet to its group under `## [Unreleased]` in
`CHANGELOG.md`, such as `Added`, `Changed`, or `Fixed`. A release raises the patch version for bug fixes, tooling, and
internal cleanups, the minor version for features, and the major version for breaking changes. While the major version
is 0, a breaking change raises the minor version. The `version` in `gradle.properties` must equal the version of the
most recent `## [x.y.z]` header, which `scripts/check-version-sync.sh` checks in CI. To release a version:

1. For every open issue and pull request that `gh issue list` and `gh pr list` show, fix the issue or merge the pull
   request, or close it with a reason.
2. Move the bullets of `## [Unreleased]` under a new `## [x.y.z] - YYYY-MM-DD` header, add the link reference
   `[x.y.z]: https://github.com/se-uhd/coffee-kitty/releases/tag/vX.Y.Z` at the bottom of `CHANGELOG.md`, and set
   `version` in `gradle.properties` to the new version.
3. Run `gradle build` and `gradle :domain:pitest`. Continue only when both pass.
4. Commit these changes as the release commit and push the commit to `main`.
5. When CI passes on the release commit, tag the commit and push the tag with
   `git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z`.

## Commits

- Write the subject as a Conventional Commit, `type(scope): description`, with at most 72 characters and one of the
  types feat, fix, docs, refactor, test, build, ci, chore, perf, and revert.
- Write a body of 1 to 5 bullets of at most 2 lines of 72 characters that state the reason for the change, then the
  trailers. End every commit message that Claude Code writes with the `Co-Authored-By:` trailer from the harness
  instructions.
- Write for a reader of `git log` who cannot read your notes. Never cite a reference that is not in the repository,
  such as the id of a review finding. Group related work into a few cohesive commits.
- `scripts/check-commit-message.sh` checks the format, `mise install` enables it as a Git hook (`mise.toml`), and
  CI checks the commits of a pull request and of a push to `main`.

## Rules for parts of the repository

Claude Code loads a rule of `.claude/rules/` when you work on a file that matches its `paths`. Read the rule before a
task that it covers, even before you open a matching file:

<!-- Generated by gradle ruleIndex from the front matter of .claude/rules/*.md. -->
- `api.md`: the controllers, the acting user, the DTOs, and the error responses.
- `build.md`: the build scripts, CI, and the thresholds of coverage, mutation tests, and duplicates.
- `data.md`: the migrations, the JPA entities, the data adapters, and the entity mappers.
- `domain.md`: the ports, the value rules, the locks, and the models that follow a domain model.
- `later-features.md`: the tables of later features and the rules for money (FR-4 to FR-6, FR-10 to FR-12, FR-15 to
  FR-19).
- `models.md`: the TLA+ and Alloy models, with `models/README.md` and `.claude/skills/statechart/SKILL.md`.
- `new-entity.md`: the steps to add an entity across the modules.
- `requirements.md`: the IDs, the key words, and the sources of the numbers in `requirements/`.
- `tests.md`: the test levels, the requests of the system tests, the test fixtures, and the test names.
<!-- End of the generated list. -->
