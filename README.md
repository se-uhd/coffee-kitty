<p align="center">
  <img src="doc/logo/coffee-kitty-logo-400.png" width="200"
       alt="Coffee Kitty logo: a coffee cup with cat ears and euro coins on Heidelberg's Old Bridge">
</p>

# Coffee Kitty

Coffee Kitty counts the coffees that the users in a group drink from a shared coffee machine. The group pays for new
coffee beans from a shared cash box, the kitty. A user can also buy coffee beans with their own money and record the
amount as an expense. So far, the app is a minimal REST API that stores users and their coffee counts, which change by
one coffee at a time and which an admin can correct. The database already has the tables for prices, payments, expenses,
beans, ratings, and balances, but no endpoint uses them yet. A user interface and authentication are still missing.

Coffee Kitty is the running example of a software engineering course at Heidelberg University. Students first see the
app in the lectures on agentic software engineering and extend it over the semester, for example with a price per cup
and a balance for each user. Coffee Kitty has the same architecture as the coffee app of [SE@UHD](https://se-uhd.de/),
the Software Engineering Group at Heidelberg University. Both apps have four backend modules, ports and adapters, and
layer rules that ArchUnit enforces, but the API of Coffee Kitty covers only three entities: users, their consumptions,
and the corrections of their coffee counts.

## Users and coffee counts

- A user has a login name, an email address, a first and a last name, and a role, `USER` or `ADMIN` (FR-14). Every
  user has one consumption, which holds the coffee count of the user. The coffee count starts at zero, and adding a
  coffee raises it by one (FR-13).
- A user can undo their most recent coffee within the grace period of 5 minutes after adding it (FR-3). The undo status
  states whether the user can undo their most recent coffee now and, if so, until when (FR-9).
- An admin can correct the coffee count of a user at any time with a note that states the reason, and can list the
  corrections of a user (FR-8). Only an admin may create a user, correct a coffee count, or list the corrections.
- A Spring Boot backend in Kotlin stores the data in PostgreSQL through Jakarta Persistence (JPA), and Flyway migrations
  define the database schema. The REST API accepts and returns only JSON.
- The migrations V6 to V12 prepare later features, such as the price per cup and the expenses, which have requirements
  in `requirements/`. `.claude/rules/later-features.md` lists what each of these features still needs.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters)
architecture. ArchUnit tests check the boundaries between the four modules:

- `domain`: the domain models, the ports, and the business logic. The module depends on Spring and on the API of Jakarta
  Bean Validation, but not on Spring MVC, JPA, or the other modules.
- `api`: the REST controllers, the data transfer objects (DTOs), the MapStruct DTO mappers, and the exception handler.
- `data`: the JPA entities and repositories, the MapStruct entity mappers, the implementations of the data ports, and
  the Flyway migrations.
- `application`: the configuration profiles and the Spring Boot application that wires the modules together. Its test
  sources hold the dev application, which creates the user fixtures.

The included build `build-logic/` holds the convention plugins, which configure Java, Kotlin, ktlint, and detekt in the
same way for every module (ADR 001 in `doc/adr/`). The other subprojects hold no app code:

- `detekt-rules`: the custom detekt rules for the comments of ADR 013, which require KDoc only on contracts such as the
  ports, limit its length, and allow no other comments except `// ADR-NNN` and `// External: <link> <reason>`.
- `coverage`: the coverage report of all modules.
- `models`: TLA+ and Alloy models of the domain, the tests that check them against the code, and the code that draws
  the models as diagrams in `doc/` (`models/README.md`).

The root build checks the text of the repository (`doc/adr/013-limits-for-text.md`). Its task `markdownLimits` checks
the Markdown files against the limits in `config/markdown/limits.yml`, and its task `detektComments` applies the comment
rules of `config/detekt/detekt.yml` to the tests and the build scripts. `gradle check` runs both tasks.

## Prerequisites

- Java 25, Gradle 9.9, and TLC, the model checker of TLA+, which [mise](https://mise.jdx.dev/) installs from
  `mise.toml`. The repository has no Gradle wrapper, and `gradle build` runs TLC on the models in `models/`.
- Docker, for the local PostgreSQL database and for the tests that use Testcontainers.

`gradle/gradle-daemon-jvm.properties` requires JDK 25 for the Gradle daemon, the background process that runs the
build, and a JDK older than 23 rejects an option in `org.gradle.jvmargs` of `gradle.properties`. In a shell with mise
activated, the daemon runs on the JDK 25 of mise. Otherwise, as in IntelliJ, Gradle does not detect the JDKs of mise
and downloads a JDK 25 into `~/.gradle/jdks` if it finds none elsewhere. `gradle updateDaemonJvm --jvm-version=25`
regenerates the properties file with the foojay toolchain resolver, which `settings.gradle.kts` applies.

## Running locally

In the repository directory, allow mise to use `mise.toml` and install the tools in the versions that it specifies. If
you have not activated mise in your shell, prefix each `gradle` command with `mise exec --`.

```shell
mise trust
mise install
```

Start PostgreSQL 18 in a Docker container named `campus-coffee-db` on host port 5433, or on the port that the
environment variable `DB_PORT` sets:

```shell
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
```

Docker keeps a stopped container, for example after a reboot, and a second `docker run` fails because the stopped
container has the same name. Start the existing container again with `docker start campus-coffee-db`.

Start the dev application, that is, the app in the `dev` profile with the user fixtures (see Test fixtures). The command
keeps running and prints the log of the app until you stop it:

```shell
gradle :application:bootTestRun
```

The dev application serves the API at `http://localhost:8081/api`. The environment variable `SERVER_PORT` overrides
the port of the app, `DB_PORT` the port of PostgreSQL, and `CAMPUS_COFFEE_GRACE_PERIOD` the grace period of 5
minutes, as in `CAMPUS_COFFEE_GRACE_PERIOD=10m`.

## REST API

In the dev profile, the app serves Swagger UI at `http://localhost:8081/api/swagger-ui.html` and the OpenAPI definition
at `http://localhost:8081/api/api-docs`. Swagger UI documents every endpoint and sends requests to the dev application.
Outside the dev profile, the app serves neither.

Coffee Kitty has no authentication yet: The header `X-Acting-User` contains the login name of the acting user, and any
client can act as any user. The endpoints that create a user, correct a coffee count, or list the corrections need an
admin as the acting user, such as `jane_doe`, and Swagger UI has the field `X-Acting-User` for them. The other endpoints
need no acting user, but a request with an unknown login name in `X-Acting-User` gets 401 Unauthorized on every
endpoint. `GLOSSARY.md` describes the body of an error response and the status of each domain exception. When no admin
exists, the app stores a bootstrap admin on startup from the environment variables `BOOTSTRAP_ADMIN_LOGIN_NAME` and
`BOOTSTRAP_ADMIN_EMAIL_ADDRESS`.

## IntelliJ IDEA

Open the root directory of the repository with `File` -> `Open`, and choose `Trust Project` when IntelliJ asks. Until
step 2, IntelliJ loads the project with a Gradle version of its choice, because the repository has no Gradle wrapper.
Ignore any offer to switch to another Gradle version. If IntelliJ creates `gradlew`, `gradlew.bat`, and
`gradle/wrapper/`, delete them, because `./gradlew` would run a different Gradle version than the one in `mise.toml`.

1. Under `File` -> `Project Structure` -> `Project`, set `SDK` to the Temurin JDK 25. If the list does not contain
   this JDK, add it with `Add SDK` and the path that `mise where java` prints. The optional
   [mise plugin](https://plugins.jetbrains.com/plugin/24904-mise) sets this SDK from `mise.toml`.
2. Under `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`, set `Distribution` to
   `Local installation`. As `Location`, enter the `gradle-9.9.x` directory that contains `bin` and `lib`, which
   `ls -d "$(mise where gradle)"/gradle-*` prints. Leave `Gradle JVM` unchanged, because the daemon runs on the JDK 25
   that `gradle/gradle-daemon-jvm.properties` requires. On macOS, `Settings` is in the `IntelliJ IDEA` menu.
3. In the Gradle tool window, click `Sync All Gradle Projects` to load the project with these settings.

## Test fixtures

On every start, the dev application deletes the rows of every table and then creates five user fixtures with a coffee
count of zero. Their login names are `jane_doe`, `maxmustermann`, `student2023`, `lisa_lee`, and `olivia_lee`, and
`jane_doe` is the only admin. The user fixtures get random ids, which change with every restart, and `GET /api/users`
lists the current ids. The tests use the same user fixtures from `TestFixtures.kt` in `domain/src/testFixtures/`. The
production jar contains neither the user fixtures nor code that deletes data, so the `main` function of the dev
application is in the test sources of the application module (`DevApplication.kt`). To keep the data of a run, start
the app without the database cleaner and the user fixtures:

```shell
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

## Testing

The domain unit tests use Mockito. Docker must be running for the data integration tests, the system tests, and the
Cucumber acceptance tests, because they run against PostgreSQL in Testcontainers. The system and acceptance tests send
JSON with literal field names, so a renamed field of the API fails them. ArchUnit tests check the layer rules, and
detekt checks the KDoc and the comments with the rules in `config/detekt/detekt.yml`.

`gradle build` compiles every module, runs ktlint, detekt, CPD, TLC, every test, and the checks of the text, and checks
the code coverage. Gradle prints `BUILD SUCCESSFUL` when every check and every test passes. The other commands run one
part of the build or, in the case of PIT, a check that `gradle build` leaves out:

```shell
gradle build                              # compile, check, and test everything except PIT
gradle test                               # the tests without ktlint, detekt, CPD, and the coverage check
gradle ktlintFormat                       # fix the formatting violations in place
gradle :coverage:testCodeCoverageReport   # the coverage report of all modules
gradle :domain:pitest                     # the mutation tests of the domain services with PIT, without Docker
gradle cpd                                # CPD, the copy-paste detector of PMD, on the production code
gradle markdownLimits                     # the limits of the Markdown files
gradle detektComments                     # the comment rules for the tests and the build scripts
```

The coverage report is `coverage/build/reports/jacoco/testCodeCoverageReport/html/index.html`, and the PIT report is
`domain/build/reports/pitest/index.html`. ADR 010 records the minimums for the coverage and the mutation score.

On every push to `main` and on every pull request, the GitHub Actions workflow `.github/workflows/build.yml` checks the
commit messages and runs `gradle build` and `gradle :domain:pitest`. Before the build, `scripts/check-version-sync.sh`
checks that the version in `gradle.properties` equals the version of the latest release in `CHANGELOG.md`. After each
push to `main`, the workflow `.github/workflows/dependency-submission.yml` submits the runtime dependencies of the
`application` module to GitHub so that Dependabot can report a vulnerable dependency.

## Commit messages

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/), with a subject such as
`fix(data): name the SQL state of a refused value` and a body of bullets (`doc/adr/013-limits-for-text.md`).
`scripts/check-commit-message.sh` checks a message. Enable it as a Git hook for every commit in your clone:

```shell
git config core.hooksPath .githooks
```

## Documentation

- `CLAUDE.md` and the rule files in `.claude/rules/`: the conventions for agents, such as Claude Code.
- `GLOSSARY.md`: one term for each concept of Coffee Kitty.
- `requirements/`: the functional requirements, one file for each, such as `FR-3-undo-coffee.md`.
- `doc/adr/`: the architecture decision records (ADRs).
- Swagger UI and the OpenAPI definition: the endpoints of the REST API (see REST API).

## License

The project uses the MIT License. See [LICENSE](LICENSE).
