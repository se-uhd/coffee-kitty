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

- A user has a login name, an email address, and a first and a last name. A login name consists of ASCII letters,
  digits, and underscores. Login names and email addresses are unique. The app stores every email address trimmed and
  in lowercase.
- Every user has one consumption, which holds the user's coffee count. When the app creates a user, it also creates the
  consumption with a coffee count of zero. Adding a coffee raises the coffee count by one. When two requests add a
  coffee for the same user at the same time, the coffee count rises by two.
- A user can undo a coffee within the grace period of 5 minutes after adding it. The undo lowers the coffee count by
  one. After an undo, the coffee before is the most recent coffee, which the user can undo while it is within its own
  grace period. The requirement is FR-3 in `requirements/FR-3-undo-coffee.md`.
- The undo status of a user states whether the user can undo their most recent coffee now and, if so, until when. A
  client reads the undo status and offers the undo only while Coffee Kitty accepts an undo. The requirement is FR-9 in
  `requirements/FR-9-undo-status.md`.
- An admin can correct the coffee count of a user at any time, even after the grace period, and has to add a note with
  the reason for the correction. A correction to a lower count removes the most recent coffees of the user, and an undo
  then applies to the most recent coffee that remains. The app stores each correction with the count before and after,
  the note, and the admin, and an admin can list the corrections of a user. The requirement is FR-8 in
  `requirements/FR-8-correct-coffee-count.md`.
- Every user has a role, `USER` or `ADMIN`. Only an admin may create a user, correct a coffee count, or list the
  corrections of a user. Coffee Kitty has no authentication yet: The header `X-Acting-User` of a request contains the
  login name of the acting user, and any client can act as any user. The user fixture `jane_doe` is an admin.
- The REST API accepts and returns only JSON.
- A Spring Boot backend in Kotlin stores the users and their coffee counts in PostgreSQL through Jakarta Persistence
  (JPA). Flyway migrations define the database schema.
- The migrations V6 to V11 add the tables of later features: the price per cup, the payments, the beans, the ratings,
  the expenses, and the balances. A payment is a deposit or a kitty adjustment, and the users and the kitty each have a
  balance. For the price, the payments, the beans, and the ratings, the data module also implements the data ports. For
  the expenses and the balances, it has only the JPA entities and the repositories. No endpoint uses these tables yet.
  The section "Tables Without an Endpoint" in `CLAUDE.md` lists what each feature still needs. The migration V12 adds a
  column for the price of each recent coffee. These features have requirements in `requirements/`: FR-4 and FR-10 for
  deposits, FR-5 for the price of a coffee, FR-11 for its undo, FR-6 for expenses, and FR-12 for ratings. FR-6 specifies
  an expense that a user paid with their own money.

## Architecture

The project is a multi-module Gradle build with Kotlin build scripts and a hexagonal (ports-and-adapters)
architecture. ArchUnit tests check the boundaries between the four modules:

- `domain`: the domain models, the ports, and the business logic. The module depends on Spring, but not on Bean
  Validation, JPA, or the other modules.
- `api`: the REST controllers, the data transfer objects (DTOs), the MapStruct DTO mappers, and the exception handler.
- `data`: the JPA entities and repositories, the MapStruct entity mappers, the implementations of the data ports, and
  the Flyway migrations.
- `application`: the Spring Boot application that wires the modules together and the configuration profiles. Its test
  sources hold the dev application, which creates the user fixtures.

The included build `build-logic/` holds the convention plugins, which configure Java, Kotlin, ktlint, and detekt in the
same way for every module (see `doc/adr/001-multi-module-gradle-build-with-convention-plugins.md`). The `detekt-rules`
subproject holds the custom detekt rules that require KDoc, and the `coverage` subproject creates the coverage report of
all modules. `CLAUDE.md` describes the architecture and the conventions in detail, and `GLOSSARY.md` defines the terms
of the project.

## Prerequisites

- Java 25 and Gradle 9.9, which [mise](https://mise.jdx.dev/) installs from `mise.toml`. The repository has no Gradle
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

Docker does not delete a stopped container, and a second `docker run` fails because the stopped container keeps its
name. The following command starts the existing container again, for example after a reboot, and prints its name:

```shell
docker start campus-coffee-db
```

Start the dev application, that is, the app in the `dev` profile with the user fixtures (see Test fixtures). The command
keeps running and prints the log of the app until you stop it:

```shell
gradle :application:bootTestRun
```

The dev application serves the API at `http://localhost:8081/api` and Swagger UI at
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
   `Local installation`. As `Location`, enter the `gradle-9.9.x` directory that contains `bin` and `lib`. The
   following command prints the path of this directory:

   ```shell
   ls -d "$(mise where gradle)"/gradle-*
   ```

   Leave `Gradle JVM` unchanged, because the daemon runs on the JDK 25 that `gradle/gradle-daemon-jvm.properties`
   requires. On macOS, `Settings` is in the `IntelliJ IDEA` menu.

3. In the Gradle tool window, click `Sync All Gradle Projects` to load the project with these settings.

## Test fixtures

On every start, the dev application deletes the rows of every table and then creates five user fixtures with a coffee
count of zero. Their login names are `jane_doe`, `maxmustermann`, `student2023`, `lisa_lee`, and `olivia_lee`. The
user fixtures get random ids, which change with every restart. `GET /api/users` lists the current ids.

`domain/src/testFixtures/kotlin/de/seuhd/campuscoffee/domain/tests/TestFixtures.kt` defines the user fixtures, a fixed
time, and consumptions in three states, and the tests of every module use them. The system and acceptance tests create
the same user fixtures before each test. The production jar contains neither the user fixtures nor code that deletes
data. The `main` function of the dev application is therefore in the test sources of the application module
(`DevApplication.kt`), and `bootTestRun` starts it. To keep the data of a run, start the app without the database
cleaner and the user fixtures:

```shell
gradle :application:bootRun --args='--spring.profiles.active=dev'
```

## REST API

The paths in this section are relative to `/api`, and Swagger UI documents every endpoint. Each command requires the
dev application on port 8081. Because the ids change with every restart, a command that needs an id first
looks up the id of the user fixture `jane_doe` with `python3`. A request with an unknown login name in the header
`X-Acting-User` gets 401 Unauthorized on every route.

`GET /users` returns all users as a JSON array, ordered by login name:

```shell
curl http://localhost:8081/api/users
```

`POST /users` with `{ "loginName", "emailAddress", "firstName", "lastName" }` and an optional `"role"` creates a user
with a coffee count of zero and returns 201 Created with the new user and a `Location` header. Only an admin may create
a user. The header `X-Acting-User` has to contain the login name of an admin. The endpoint returns 401 Unauthorized
without the header or for an unknown login name, and 403 Forbidden when the acting user is not an admin. It returns 400
Bad Request for a missing or malformed field, such as a number for the login name or the role. It returns the same
status for an unknown field, such as an `id`, and for a value that breaks a rule, such as a login name with a space. It
returns 409 Conflict when another user has the login name or the email address. A request without a known acting user
gets 401 even with an invalid body, because a filter checks the acting user before the controller reads the body. Every
401 has the header `WWW-Authenticate: X-Acting-User realm="Coffee Kitty"`, as RFC 9110 requires. The dev application
creates `jane_doe` as an admin. When no admin exists, the app stores a bootstrap admin on startup from the environment
variables `BOOTSTRAP_ADMIN_LOGIN_NAME` and `BOOTSTRAP_ADMIN_EMAIL_ADDRESS` (optional: `BOOTSTRAP_ADMIN_FIRST_NAME` and
`BOOTSTRAP_ADMIN_LAST_NAME`). If a user has that login name, the app gives that user the role `ADMIN`. This case occurs
in a database that the dev application has not cleaned, because the migration V4 gives every existing user, `jane_doe`
included, the role `USER`. With `jane_doe` as the acting user, the command creates the user `ada_lovelace`:

```shell
curl --include --request POST --header "Content-Type: application/json" --header "X-Acting-User: jane_doe" \
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
coffee without changing the format of the request body. The endpoint returns 400 Bad Request for any delta other than 1
and for a body without a delta:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --request POST --header "Content-Type: application/json" --data '{"delta":1}' \
  http://localhost:8081/api/users/$USER_ID/consumption
```

`POST /users/{userId}/consumption/undo` undoes the most recent coffee of the user and returns the new coffee count as
`{ "loginName", "count" }`. The endpoint returns 409 Conflict when the user has no coffee to undo or when the grace
period of the most recent coffee that the user did not undo has passed. The environment variable
`CAMPUS_COFFEE_GRACE_PERIOD` sets the grace period, 5 minutes by default, as in `CAMPUS_COFFEE_GRACE_PERIOD=10m`. Run
the command within the grace period after the command that adds a coffee:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --request POST http://localhost:8081/api/users/$USER_ID/consumption/undo
```

`GET /users/{userId}/consumption/undo` returns the undo status of the user as `{ "undoable", "undoableUntil" }`.
`undoable` is true when the user can undo their most recent coffee now, that is, when a `POST` request to the same path
would succeed. `undoableUntil` is then the end of the grace period of that coffee, and null when `undoable` is false:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl http://localhost:8081/api/users/$USER_ID/consumption/undo
```

`PUT /users/{userId}/consumption` with `{ "total", "note" }` corrects the coffee count of the user to the total, stores
the correction with its note, and returns the corrected coffee count as `{ "loginName", "count" }`. Only an admin may
correct a coffee count. The header `X-Acting-User` has to contain the login name of an admin. The endpoint returns 401
Unauthorized without the header or for an unknown login name, and 403 Forbidden when the acting user is not an admin. It
returns 400 Bad Request for a missing or negative total, for a total above 1,000,000, and for a missing or blank note. A
note has at most 500 characters. These limits and the rule that only an admin may read the corrections hold until the
group lead answers the TBDs of FR-8. The command sets the coffee count of `jane_doe` to 1:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --request PUT --header "Content-Type: application/json" --header "X-Acting-User: jane_doe" \
  --data '{"total":1,"note":"Forgot to add a coffee on 16.11."}' \
  http://localhost:8081/api/users/$USER_ID/consumption
```

`GET /users/{userId}/consumption/corrections` returns the corrections of the coffee count of the user, newest first, as
a JSON array of `{ "countBefore", "countAfter", "note", "adminLoginName", "createdAt" }`. Only an admin may read the
corrections. The endpoint returns 401 Unauthorized without the header `X-Acting-User` or for an unknown login name and
403 Forbidden when the acting user is not an admin:

```shell
USER_ID=$(curl -s http://localhost:8081/api/users |
  python3 -c 'import json, sys; print(next(u["id"] for u in json.load(sys.stdin) if u["loginName"] == "jane_doe"))')
curl --header "X-Acting-User: jane_doe" http://localhost:8081/api/users/$USER_ID/consumption/corrections
```

The consumption endpoints return 404 Not Found for an unknown user. An error response has a JSON body with the fields
`errorCode`, `message`, `statusCode`, `statusMessage`, `timestamp`, and `path`. For a domain exception, `errorCode` is
the class name of the exception: `NotFoundException`, `DuplicationException`, `ConflictException`, `ForbiddenException`,
or `ValidationException`. For any other error, `errorCode` is the reason phrase of the status without spaces, such as
`BadRequest`. A 400 has the error code `BadRequest` when the app cannot read the request: A field is missing, has the
wrong JSON type, or is unknown. It has the error code `ValidationException` when a value breaks a rule, such as a login
name of 256 characters or a blank note. Every timestamp in a response is in UTC and has no offset.

## Testing

The domain unit tests use Mockito. Docker must be running for the data integration tests, the system tests, and the
Cucumber acceptance tests, because they run against PostgreSQL in Testcontainers. The system and acceptance tests send
JSON with literal field names and parse the responses into classes of their own, so a renamed field of the API fails
them. ArchUnit tests check the layer rules, and detekt checks that the classes and functions of the main sources have
KDoc.

Compile every module, run ktlint, detekt, CPD, and every test, and check the code coverage. Gradle prints
`BUILD SUCCESSFUL` when every check and every test passes:

```shell
gradle build
```

Run the tests without ktlint, detekt, CPD, and the check of the code coverage:

```shell
gradle test
```

Let ktlint fix formatting violations in place:

```shell
gradle ktlintFormat
```

Create the coverage report of all modules, `coverage/build/reports/jacoco/testCodeCoverageReport/html/index.html`.
`gradle build` also creates it. The build fails when the tests cover less than 95% of the lines or 85% of the branches.
It also fails when the tests do not execute every function of the domain services and the controllers:

```shell
gradle :coverage:testCodeCoverageReport
```

Run the mutation tests of the domain services. PIT changes the code, for example by negating a condition, and runs the
unit tests of the domain module against each changed version (mutant). The report
`domain/build/reports/pitest/index.html` lists the mutants that no test detects. The task needs no Docker and fails when
the tests detect fewer than 90% of all mutants. `gradle build` does not run it, and the CI workflow runs it after the
build:

```shell
gradle :domain:pitest
```

Search the production code for duplicates with CPD, the copy-paste detector of PMD. `gradle build` also runs it:

```shell
gradle cpd
```

On every push to `main` and on every pull request, the GitHub Actions workflow `.github/workflows/build.yml` runs
`gradle build` and `gradle :domain:pitest`. Before the build, `scripts/check-version-sync.sh` checks that the version in
`gradle.properties` equals the version of the latest release in `CHANGELOG.md`. After each push to `main`, the workflow
`.github/workflows/dependency-submission.yml` submits the runtime dependencies of the `application` module to GitHub so
that Dependabot can report a vulnerable dependency.

## License

The project uses the MIT License. See [LICENSE](LICENSE).
