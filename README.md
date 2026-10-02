# Campus Coffee Consumption

This repository, [se-uhd/coffee-kitty](https://github.com/se-uhd/coffee-kitty), holds the teaching core of
CampusCoffeeConsumption, the app that tracks the coffee consumption at [SE@UHD](https://se-uhd.de/), the Software
Engineering Group at Heidelberg University. The core is a minimal coffee counter that a software engineering lecture
uses as its running example. Students start from it in the first lecture, which covers build automation and
continuous integration (CI) with Gradle and GitHub Actions, and extend it over the semester. The core keeps the
architecture of CampusCoffeeConsumption (e.g., the four backend modules, ports and adapters, and the layer rules that
ArchUnit enforces) with a feature set small enough to read in one sitting.

## Users and coffee counts

- A user has a login name, an email address, and a first and a last name. The login name consists of letters, digits,
  and underscores. Login names and email addresses are unique, and the app stores each address trimmed and in
  lowercase.
- Every user has one consumption, which holds the user's coffee count. The app creates the consumption with a count of
  zero together with the user. Adding a coffee raises the count by one, and nothing lowers it.
- The REST API accepts and returns JSON only. Every endpoint is open, because the core has no authentication.
- A Spring Boot backend in Kotlin stores the users and their coffee counts in PostgreSQL through Jakarta Persistence
  (JPA), and Flyway migrations define the schema.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters)
architecture. ArchUnit tests check the boundaries between its four modules:

- `domain`: the domain models, the port interfaces, and the business logic. It depends on Spring, but not on Bean
  Validation, JPA, or the other modules.
- `api`: the REST controllers, the data transfer objects (DTOs), the MapStruct DTO mappers, and the exception handler.
- `data`: the JPA entities and repositories, the MapStruct entity mappers, the implementations of the data ports, and
  the Flyway migrations.
- `application`: the Spring Boot application that wires the modules together, with the configuration profiles and the
  fixture loader.

The included build `build-logic/` holds the convention plugins that share the Java, Kotlin, ktlint, and detekt setup
across the modules (see `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md`). The `detekt-rules`
subproject holds the custom detekt rules that require KDoc. `CLAUDE.md` describes the architecture and the conventions
in detail, and `GLOSSARY.md` defines the vocabulary.

## Prerequisites

- Java 25 and Gradle 9.5, provisioned by [mise](https://mise.jdx.dev/) from `mise.toml`. There is no Gradle wrapper.
- Docker, for the local PostgreSQL database and for the tests that use Testcontainers.

## Running locally

Clone the repository and install the tools:

```shell
git clone https://github.com/se-uhd/coffee-kitty.git
cd coffee-kitty
mise install
```

Start PostgreSQL 18:

```shell
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
```

Run the application in the `dev` profile:

```shell
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

In a shell without mise activated, prefix each `gradle` command with `mise exec --`. The `dev` profile serves the API
at `http://localhost:8081/api`, with Swagger UI at `http://localhost:8081/api/swagger-ui.html`. It listens on `:8081`
rather than the conventional `:8080` to avoid a collision with another local app (override the port with
`SERVER_PORT`), and it expects PostgreSQL on port 5433 (override with `DB_PORT`).

## Test fixtures (dev)

On every start, the `dev` profile clears the data and seeds five users, each with a coffee count of zero. Their login
names are `jane_doe`, `maxmustermann`, `student2023`, `lisa_lee`, and `olivia_lee`. The app assigns random ids, which
change with every restart, and `GET /api/users` lists the current ids. The system and acceptance tests seed the same
users, which `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/tests/TestFixtures.kt` defines.

## REST API

All paths are under `/api`, and Swagger UI shows the full contract.

- `GET /users`: all users, ordered by login name.
- `POST /users` `{ "loginName", "emailAddress", "firstName", "lastName" }`: create a user with a coffee count of zero.
  The response is 201 Created with a `Location` header, or 409 Conflict when the login name or the email address is
  taken.
- `GET /users/{id}`: one user, or 404 Not Found for an unknown id.
- `GET /users/{userId}/consumption`: the user's coffee count as `{ "loginName", "count" }`.
- `POST /users/{userId}/consumption` `{ "delta": 1 }`: add one coffee and return the new count. Any other delta
  returns 400 Bad Request, and so does a body without a delta.

Both consumption endpoints return 404 Not Found for an unknown user. An error response is a JSON body with the fields
`errorCode`, `message`, `statusCode`, `statusMessage`, `timestamp`, and `path`. The `errorCode` names the domain
exception (i.e., `NotFoundException`, `DuplicationException`, or `ValidationException`) or, for any other error, the
status (e.g., `BadRequest`).

## Testing

```shell
gradle build   # compiles every module, runs ktlint and detekt, and runs every test
gradle test    # runs the tests only
```

The domain unit tests use Mockito. The data integration test, the system tests, and the Cucumber acceptance tests run
against PostgreSQL in Testcontainers, so Docker must be running. ArchUnit tests check the layer rules, and detekt
checks that the classes and functions of the main sources have KDoc. `gradle ktlintFormat` applies the formatting
fixes. GitHub Actions (`.github/workflows/build.yml`) runs the same build on every push to `main` and on every pull
request, after `scripts/check-version-sync.sh` checks that `gradle.properties` and `CHANGELOG.md` name the same
version.

## License

The project uses the MIT License. See [LICENSE](LICENSE).
