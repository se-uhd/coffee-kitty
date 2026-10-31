<p align="center">
  <img src="doc/logo/coffee-kitty-logo-400.png" width="200"
       alt="Coffee Kitty logo: a coffee cup with cat ears and euro coins on Heidelberg's Old Bridge">
</p>

# Coffee Kitty

Coffee Kitty counts the coffees that group members drink from a shared coffee machine. The group buys new coffee beans
from a shared cash box, the kitty, and members can also buy coffee themselves and declare the purchase as an expense.
The app is a minimal REST API for users, each with a coffee count that grows by one coffee at a time, and it has no
prices, payments, expenses, user interface, or authentication yet. A software engineering lecture at Heidelberg
University uses it as its running example. Students first see it in the lectures on agentic software engineering and
extend it over the semester, for example with a price per cup and a balance for each user. It has the architecture of
the coffee app of [SE@UHD](https://se-uhd.de/), the Software Engineering Group at Heidelberg University, but only two
entities: users and their consumptions. That architecture includes the four backend modules, ports and adapters, and
the layer rules that ArchUnit enforces.

## Users and coffee counts

- A user has a login name, an email address, and a first and a last name. The login name consists of ASCII letters,
  digits, and underscores. Login names and email addresses are unique, and the app stores each address trimmed and in
  lowercase.
- Every user has one consumption, which holds the user's coffee count. The app creates the consumption with a count of
  zero together with the user. Adding a coffee raises the count by one, and nothing lowers it. When two requests add a
  coffee for the same user at the same time, both coffees count.
- The REST API accepts and returns JSON only. Every endpoint is open, because Coffee Kitty has no authentication.
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

- Java 25 and Gradle 9.8, provisioned by [mise](https://mise.jdx.dev/) from `mise.toml`. There is no Gradle wrapper.
- Docker, for the local PostgreSQL database and for the tests that use Testcontainers.

The Gradle daemon, the background process that runs the build, needs JDK 25, because `org.gradle.jvmargs` in
`gradle.properties` passes it options that older JDKs reject. `gradle/gradle-daemon-jvm.properties` requires that
version. If Gradle is started with JDK 25, as in a shell with mise, the daemon uses that JDK. Otherwise, Gradle looks
for a JDK 25 on the machine and, through the foojay toolchain resolver in `settings.gradle.kts`, downloads one into
`~/.gradle/jdks` if it finds none. Gradle does not detect JDKs installed by mise, so IntelliJ may trigger this download
even if mise has installed JDK 25. `gradle updateDaemonJvm --jvm-version=25` regenerates the file.

## Running locally

Clone the repository and install the tools:

```shell
git clone https://github.com/se-uhd/coffee-kitty.git
cd coffee-kitty
mise install
```

If mise is not activated in your shell, prefix each `gradle` command with `mise exec --`.

Start PostgreSQL 18:

```shell
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
```

Docker keeps the container after it stops. To start it again later, for example after a reboot, run the following
command, because running `docker run` again fails on the taken container name:

```shell
docker start campus-coffee-db
```

Run the application in the `dev` profile:

```shell
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

The `dev` profile serves the API at `http://localhost:8081/api`, with Swagger UI at
`http://localhost:8081/api/swagger-ui.html`. It listens on `:8081` rather than the conventional `:8080` to avoid a
collision with another local app (override the port with `SERVER_PORT`), and it connects to PostgreSQL on port 5433
(override with `DB_PORT`).

## IntelliJ IDEA

Open the root directory of the repository via `File` -> `Open`, and choose `Trust Project` when IntelliJ asks. Because
the repository has no Gradle wrapper, IntelliJ loads the project with a Gradle version of its own choice until you set
the local installation in step 2, and it may create the wrapper files `gradlew`, `gradlew.bat`, and `gradle/wrapper/`.
`.gitignore` lists these files. Delete them, because `./gradlew` would run a different Gradle version than the one in
`mise.toml`. If IntelliJ offers to switch to another Gradle version, ignore the offer and set the local installation.

1. Under `File` -> `Project Structure` -> `Project`, set `SDK` to the Temurin JDK 25. If it is not listed, add it via
   `Add SDK` with the path that `mise where java` prints. The optional
   [mise plugin](https://plugins.jetbrains.com/plugin/24904-mise) sets this SDK from `mise.toml`.
2. Under `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`, set `Distribution` to
   `Local installation`. As `Location`, enter the path that `ls -d "$(mise where gradle)"/gradle-*` prints, that is,
   the `gradle-9.8.x` directory that contains `bin` and `lib`. `Gradle JVM` can stay as it is, because the daemon runs
   on the JDK 25 from `gradle/gradle-daemon-jvm.properties`. On macOS, `Settings` is in the `IntelliJ IDEA` menu.
3. In the Gradle tool window, click `Sync All Gradle Projects` to load the project with these settings.

## Test fixtures (dev)

On every start, the `dev` profile clears the data and seeds five users, each with a coffee count of zero. Their login
names are `jane_doe`, `maxmustermann`, `student2023`, `lisa_lee`, and `olivia_lee`. The app assigns random ids, which
change with every restart, and `GET /api/users` lists the current ids. The system and acceptance tests seed the same
users, which `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/tests/TestFixtures.kt` defines.

## REST API

All paths are under `/api`, and Swagger UI shows the full contract. Each command needs the app running in the `dev`
profile on port 8081. The ids change with every restart, so a command that needs one first looks up the id of the
fixture user `jane_doe` with `python3`.

`GET /users` lists all users, ordered by login name:

```shell
curl http://localhost:8081/api/users
```

`POST /users` with `{ "loginName", "emailAddress", "firstName", "lastName" }` creates a user with a coffee count of
zero. It returns 201 Created with a `Location` header, 400 Bad Request for a missing or malformed field or an `id` in
the body, or 409 Conflict when the login name or the email address is taken:

```shell
curl --include --request POST --header "Content-Type: application/json" \
  --data '{"loginName":"ada_lovelace","emailAddress":"ada.lovelace@example.com","firstName":"Ada","lastName":"Lovelace"}' \
  http://localhost:8081/api/users
```

`GET /users/{id}` reads one user, or returns 404 Not Found for an unknown id and 400 Bad Request for an id that is not
a UUID:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl http://localhost:8081/api/users/$USER_ID
```

`GET /users/{userId}/consumption` reads the user's coffee count as `{ "loginName", "count" }`:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl http://localhost:8081/api/users/$USER_ID/consumption
```

`POST /users/{userId}/consumption` with `{ "delta": 1 }` adds one coffee and returns the new count. The request carries
a delta instead of always adding one coffee, so that an extension can accept a delta of -1 to remove a coffee without
changing the request body. Any other delta returns 400 Bad Request, and so does a body without a delta:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --request POST --header "Content-Type: application/json" --data '{"delta":1}' \
  http://localhost:8081/api/users/$USER_ID/consumption
```

Both consumption endpoints return 404 Not Found for an unknown user. An error response has a JSON body with the fields
`errorCode`, `message`, `statusCode`, `statusMessage`, `timestamp`, and `path`. The `errorCode` is the class name of the
domain exception (i.e., `NotFoundException`, `DuplicationException`, or `ValidationException`) or, for any other error,
the reason phrase of the status without spaces (e.g., `BadRequest`). Every timestamp of the API is in UTC, without an
offset.

## Testing

```shell
gradle build   # compiles every module, runs ktlint and detekt, and runs every test
gradle test    # runs the tests only
```

The domain unit tests use Mockito. The data integration tests, the system tests, and the Cucumber acceptance tests run
against PostgreSQL in Testcontainers, so Docker must be running. ArchUnit tests check the layer rules, and detekt checks
that the classes and functions of the main sources have KDoc. `gradle ktlintFormat` applies the formatting fixes.

GitHub Actions (`.github/workflows/build.yml`) runs the same build on every push to `main` and on every pull request,
after `scripts/check-version-sync.sh` checks that `gradle.properties` and `CHANGELOG.md` name the same version. After
each push to `main`, `.github/workflows/dependency-submission.yml` submits the application's runtime dependencies to
GitHub, so that Dependabot reports a vulnerable dependency.

## License

The project uses the MIT License. See [LICENSE](LICENSE).
