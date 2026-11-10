<p align="center">
  <img src="doc/logo/coffee-kitty-logo-400.png" width="200"
       alt="Coffee Kitty logo: a coffee cup with cat ears and euro coins on Heidelberg's Old Bridge">
</p>

# Coffee Kitty

Coffee Kitty counts the coffees that the users in a group drink from a shared coffee machine. The group buys new coffee
beans from a shared cash box, the kitty. A user can also buy coffee with their own money and declare the purchase as an
expense. So far, the app is a minimal REST API that stores users and their coffee counts, which grow by one coffee at a
time. Prices, payments, expenses, a user interface, and authentication are still missing.

Coffee Kitty is the running example of a software engineering course at Heidelberg University. Students first see the
app in the lectures on agentic software engineering and extend it over the semester, for example with a price per cup
and a balance for each user. Coffee Kitty has the same architecture as the coffee app of [SE@UHD](https://se-uhd.de/),
the Software Engineering Group at Heidelberg University. Both apps have four backend modules, ports and adapters, and
layer rules that ArchUnit enforces, but Coffee Kitty has only two entities: users and their consumptions.

## Users and coffee counts

- A user has a login name, an email address, and a first and a last name. A login name consists of ASCII letters,
  digits, and underscores. Login names and email addresses are unique. The app stores every email address trimmed and
  in lowercase.
- Every user has one consumption, which holds the user's coffee count. When the app creates a user, it also creates
  the consumption with a coffee count of zero. Adding a coffee raises the coffee count by one, and no endpoint lowers
  it. When two requests add a coffee for the same user at the same time, both coffees count.
- The REST API accepts and returns only JSON. Any client can call every endpoint, because Coffee Kitty has no
  authentication.
- A Spring Boot backend in Kotlin stores the users and their coffee counts in PostgreSQL through Jakarta Persistence
  (JPA). Flyway migrations define the database schema.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters)
architecture. ArchUnit tests check the boundaries between the four modules:

- `domain`: the domain models, the ports, and the business logic. The module depends on Spring, but not on Bean
  Validation, JPA, or the other modules.
- `api`: the REST controllers, the data transfer objects (DTOs), the MapStruct DTO mappers, and the exception handler.
- `data`: the JPA entities and repositories, the MapStruct entity mappers, the implementations of the data ports, and
  the Flyway migrations.
- `application`: the Spring Boot application that wires the modules together, the configuration profiles, and the
  fixture loader.

The included build `build-logic/` holds the convention plugins, which configure Java, Kotlin, ktlint, and detekt in the
same way for every module (see `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md`). The `detekt-rules`
subproject holds the custom detekt rules that require KDoc. `CLAUDE.md` describes the architecture and the conventions
in detail, and `GLOSSARY.md` defines the terms of the project.

## Prerequisites

- Java 25 and Gradle 9.8, which [mise](https://mise.jdx.dev/) installs from `mise.toml`. The repository has no Gradle
  wrapper.
- Docker, for the local PostgreSQL database and for the tests that use Testcontainers.

`gradle/gradle-daemon-jvm.properties` requires JDK 25 for the Gradle daemon, the background process that runs the
build. A JDK older than 23 cannot start the daemon, because the JDK rejects the option
`--sun-misc-unsafe-memory-access=allow`, which `org.gradle.jvmargs` in `gradle.properties` passes to the daemon.

When you start Gradle with JDK 25, as in a shell with mise activated, the daemon runs on that JDK. Otherwise, Gradle
looks for a JDK 25 on the machine. If Gradle finds none, it downloads a JDK 25 into `~/.gradle/jdks` from the URLs in
`gradle/gradle-daemon-jvm.properties`. IntelliJ may trigger this download even when mise has installed JDK 25, because
Gradle does not detect the JDKs that mise installs. `gradle updateDaemonJvm --jvm-version=25` regenerates the properties
file and needs the foojay toolchain resolver in `settings.gradle.kts` to look up the download URLs.

## Running locally

In the repository directory, allow mise to use `mise.toml`:

```shell
mise trust
```

Then install Java and Gradle in the versions that `mise.toml` specifies:

```shell
mise install
```

If you have not activated mise in your shell, prefix each `gradle` command with `mise exec --`.

Start PostgreSQL 18 in a Docker container named `campus-coffee-db` on host port 5433, or on the port that the
environment variable `DB_PORT` sets. The command prints the id of the new container:

```shell
docker run -d --name campus-coffee-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p ${DB_PORT:-5433}:5432 postgres:18-alpine
```

Docker keeps the container when it stops, and a second `docker run` fails because the container name is taken. The
following command starts the existing container again, for example after a reboot, and prints its name:

```shell
docker start campus-coffee-db
```

Start the app in the `dev` profile. The command keeps running and prints the log of the app until you stop it:

```shell
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

In the `dev` profile, the app serves the API at `http://localhost:8081/api` and Swagger UI at
`http://localhost:8081/api/swagger-ui.html`. The app uses port 8081 instead of the default 8080 to avoid a conflict
with another local app, and it connects to PostgreSQL on port 5433. The environment variable `SERVER_PORT` overrides
the port of the app, and `DB_PORT` overrides the port of PostgreSQL.

## IntelliJ IDEA

Open the root directory of the repository with `File` -> `Open`, and choose `Trust Project` when IntelliJ asks.
Without a Gradle wrapper in the repository, IntelliJ loads the project with a Gradle version of its choice until you
select the local Gradle installation in step 2. IntelliJ may also create the wrapper files `gradlew`, `gradlew.bat`, and
`gradle/wrapper/`. Git ignores these files, because `.gitignore` lists them. Delete them nevertheless, because
`./gradlew` would run a different Gradle version than the version in `mise.toml`. If IntelliJ offers to switch to
another Gradle version, ignore the offer and select the local installation.

1. Under `File` -> `Project Structure` -> `Project`, set `SDK` to the Temurin JDK 25. If the list does not contain
   this JDK, add it with `Add SDK` and the path that the following command prints:

   ```shell
   mise where java
   ```

   The optional [mise plugin](https://plugins.jetbrains.com/plugin/24904-mise) sets this SDK from `mise.toml`.

2. Under `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`, set `Distribution` to
   `Local installation`. As `Location`, enter the `gradle-9.8.x` directory that contains `bin` and `lib`. The
   following command prints the path of this directory:

   ```shell
   ls -d "$(mise where gradle)"/gradle-*
   ```

   Leave `Gradle JVM` unchanged, because the daemon runs on the JDK 25 that `gradle/gradle-daemon-jvm.properties`
   requires. On macOS, `Settings` is in the `IntelliJ IDEA` menu.

3. In the Gradle tool window, click `Sync All Gradle Projects` to load the project with these settings.

## Test fixtures (dev)

On every start in the `dev` profile, the fixture loader deletes all users and consumptions and then creates five
fixture users with a coffee count of zero. Their login names are `jane_doe`, `maxmustermann`, `student2023`,
`lisa_lee`, and `olivia_lee`. The fixture users get random ids, which change with every restart. `GET /api/users` lists
the current ids. `domain/src/main/kotlin/de/seuhd/campuscoffee/domain/tests/TestFixtures.kt` defines the fixture users,
and the system and acceptance tests create the same users before each test.

## REST API

The paths in this section are relative to `/api`, and Swagger UI documents every endpoint. Each command requires the
app to run in the `dev` profile on port 8081. Because the ids change with every restart, a command that needs an id
first looks up the id of the fixture user `jane_doe` with `python3`.

`GET /users` returns all users as a JSON array, ordered by login name:

```shell
curl http://localhost:8081/api/users
```

`POST /users` with `{ "loginName", "emailAddress", "firstName", "lastName" }` creates a user with a coffee count of
zero and returns 201 Created with the new user and a `Location` header. The endpoint returns 400 Bad Request for a
missing or malformed field or an `id` in the body, and 409 Conflict when the login name or the email address is taken:

```shell
curl --include --request POST --header "Content-Type: application/json" \
  --data '{"loginName":"ada_lovelace","emailAddress":"ada.lovelace@example.com","firstName":"Ada","lastName":"Lovelace"}' \
  http://localhost:8081/api/users
```

`GET /users/{id}` returns the user with this id. The endpoint returns 404 Not Found for an unknown id and 400 Bad
Request for an id that is not a UUID:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl http://localhost:8081/api/users/$USER_ID
```

`GET /users/{userId}/consumption` returns the coffee count of the user as `{ "loginName", "count" }`:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl http://localhost:8081/api/users/$USER_ID/consumption
```

`POST /users/{userId}/consumption` with `{ "delta": 1 }` adds one coffee and returns the new coffee count as
`{ "loginName", "count" }`. The request body contains a delta so that an extension can accept a delta of -1 to remove a
coffee without changing the request body. The endpoint returns 400 Bad Request for any other delta and for a body
without a delta:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --request POST --header "Content-Type: application/json" --data '{"delta":1}' \
  http://localhost:8081/api/users/$USER_ID/consumption
```

Both consumption endpoints return 404 Not Found for an unknown user. An error response has a JSON body with the fields
`errorCode`, `message`, `statusCode`, `statusMessage`, `timestamp`, and `path`. For a domain exception, `errorCode` is
the class name of the exception: `NotFoundException`, `DuplicationException`, or `ValidationException`. For any other
error, `errorCode` is the reason phrase of the status without spaces, such as `BadRequest`. Every timestamp in a
response is in UTC and has no offset.

## Testing

The domain unit tests use Mockito. Docker must be running for the data integration tests, the system tests, and the
Cucumber acceptance tests, because they run against PostgreSQL in Testcontainers. ArchUnit tests check the layer rules,
and detekt checks that the classes and functions of the main sources have KDoc.

Compile every module, run ktlint and detekt, and run every test. Gradle prints `BUILD SUCCESSFUL` when every check
and every test passes:

```shell
gradle build
```

Run the tests without ktlint and detekt:

```shell
gradle test
```

Let ktlint fix formatting violations in place:

```shell
gradle ktlintFormat
```

On every push to `main` and on every pull request, the GitHub Actions workflow `.github/workflows/build.yml` runs
`gradle build`. Before the build, `scripts/check-version-sync.sh` checks that `gradle.properties` and the latest release
in `CHANGELOG.md` name the same version. After each push to `main`, the workflow
`.github/workflows/dependency-submission.yml` submits the runtime dependencies of the `application` module to GitHub so
that Dependabot can report a vulnerable dependency.

## License

The project uses the MIT License. See [LICENSE](LICENSE).
