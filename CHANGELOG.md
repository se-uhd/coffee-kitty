# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.3] - 2026-11-22

### Fixed

- A request with a wildcard in its `Content-Type`, such as `*/*` or `application/*`, gets a 415 with the error code
  `UnsupportedMediaType`. Until now it got a 500, because Spring rejects such a header with an
  `IllegalArgumentException` while it reads the request, and `GlobalExceptionHandler` mapped that exception like a
  failed `require`.

## [0.2.2] - 2026-11-22

### Fixed

- `POST /api/users/{userId}/consumption` rejects a delta that is a JSON string, such as `{"delta": "1"}`, with a 400
  and the error code `BadRequest`. Until now Jackson read the string as the number 1, and the request added a coffee.
  `application.yaml` sets `spring.jackson.mapper.allow-coercion-of-scalars` to false, so the same holds for every
  number and boolean field of a request body.
- `POST /api/users` rejects a JSON number for the role, such as `"role": 1`, and a JSON number or boolean for a
  `String` field, such as `"loginName": 12345`, with a 400 and the error code `BadRequest`. Until now Jackson read the
  number 1 as the role `ADMIN`, the constant with the index 1, and stored a number or a boolean as its text, such as
  the login name "12345". `application.yaml` sets `spring.jackson.datatype.enum.fail-on-numbers-for-enums` to true for
  every enum field of a request body, and the new `JsonConfig` rejects a number or a boolean for every `String` field,
  for which Spring Boot has no property.
- The REST API reads and writes only JSON, as the README states. A request with a YAML body, such as `POST /api/users`
  with `Content-Type: application/yaml`, gets a 415 with the error code `UnsupportedMediaType`, and a request with
  `Accept: application/yaml` gets a 406. Until now such a body got a 500, and such an `Accept` header got a YAML
  response, because springdoc puts the YAML module of Jackson on the classpath and Spring registered a YAML message
  converter for it. `ApiWebConfig` removes this message converter as it removes the XML message converters.

## [0.2.1] - 2026-11-17

### Added

- GLOSSARY.md defines the terms correction and note of FR-8.
- `SecurityConfig` requires an acting user for the requests of FR-8, `PUT /api/users/{userId}/consumption` and
  `GET /api/users/{userId}/consumption/corrections`, so that an implementation of FR-8 does not have to change the
  security configuration. Until Coffee Kitty implements FR-8, a request with an acting user gets 405 or 404.

### Changed

- The step definitions of FR-3 in `CucumberConsumptionSteps` send their requests through `ConsumptionRequests`
  instead of building HTTP requests, and their Then steps check the stored coffee count.
- The system tests send their requests through the new `UserRequests` and through new functions of
  `ConsumptionRequests` instead of building HTTP requests. These functions return a `Response` with the status code,
  the headers, and the body or the error response. Only the tests of an unknown path, of a path that the firewall
  rejects, and of the OpenAPI definition build their own requests.

### Fixed

- FR-8 names the TBD of FR-3 that it resolves by its question instead of a wrong position and quotes the decision of
  the group lead as its business requirement. Its Rationale no longer states who reads the notes, which a TBD of FR-8
  leaves open.

## [0.2.0] - 2026-11-17

### Added

- A user can undo their most recent coffee within 5 minutes of adding it (FR-3 in `requirements/FR-3-undo-coffee.md`).
  `POST /api/users/{userId}/consumption/undo` lowers the coffee count by one and returns 409 Conflict when the user
  has no coffee to undo or when the grace period of the most recent coffee has ended. After an undo, the coffee before
  can be undone while its own grace period is open. The grace period includes its last instant, as in the coffee app of
  SE@UHD, and `campus-coffee.consumption.grace-period` sets it (default 5 minutes). The migration V3 creates the
  table `coffee_times`, which holds the time of each recent coffee that is not undone. Adding a coffee drops the times
  whose grace period has passed. Coffees added before this release have no time and cannot be undone.
- Every user has a role, `USER` or `ADMIN` (migration V4), and the fixture user `jane_doe` is an admin. `POST
  /api/users` may set the role of the new user, which defaults to `USER`.
- Spring Security runs `ActingUserAuthenticationFilter` before the controllers. The filter resolves the acting user
  from the header `X-Acting-User`, and `SecurityConfig` lists the routes that need one. A request without a known acting
  user gets 401 Unauthorized before Spring MVC reads its body, and every 401 has the header `WWW-Authenticate` with the
  challenge `X-Acting-User realm="Coffee Kitty"`, as RFC 9110 requires.
- FR-8 in `requirements/FR-8-correct-coffee-count.md` specifies the correction of a coffee count by an admin, which
  the group lead decided and which assignment 5 implements. FR-3 marks its open question about corrections as
  resolved.
- `ConsumptionRequests` in the tests sends the requests of the step definitions, such as adding a coffee or reading a
  coffee count, so that a step names users by login name and gets status codes and values back. Its functions for
  FR-8 send the correction and read the corrections, which Coffee Kitty implements only with assignment 5.
- When no admin exists, `BootstrapAdminLoader` stores an admin on startup, after the fixture loader, from the keys
  under `campus-coffee.bootstrap-admin`, which the environment variables `BOOTSTRAP_ADMIN_LOGIN_NAME`,
  `BOOTSTRAP_ADMIN_EMAIL_ADDRESS`, `BOOTSTRAP_ADMIN_FIRST_NAME`, and `BOOTSTRAP_ADMIN_LAST_NAME` set. If a user has the
  configured login name, the loader gives that user the role `ADMIN`.

### Changed

- Breaking: only an admin may create a user with `POST /api/users`. Coffee Kitty has no authentication yet. A client
  sends the login name of its acting user in the header `X-Acting-User`. The response is 401 Unauthorized without a
  known acting user and 403 Forbidden when the acting user is not an admin. The migration V4 gives every existing user
  the role `USER`, including `jane_doe` in a database that the `dev` profile does not reset. Configure the bootstrap
  admin with the login name of an existing user, or of a new one, before creating users.
- The default firewall of Spring Security rejects a path with `;`, `//`, `..`, or an encoded slash and a header value
  with a control character, because security rules and Spring MVC can read such a request differently. A rejected
  request gets 400 Bad Request with the message "The request could not be read." Coffee Kitty 0.1.0 handled `GET
  /api/users;x=1` like `GET /api/users` and responded to `GET /api//users` with 404 Not Found.
- The README describes how the group buys coffee beans from the kitty and how a user who buys coffee with their own
  money declares the purchase as an expense. It states that students first see the app in the lectures on agentic
  software engineering and explains how to set up IntelliJ IDEA.
- The README no longer tells readers to clone the repository, because they have already cloned it when they open the
  README in IntelliJ IDEA. The setup instructions start with `mise trust` and continue with `mise install`, and every
  command in the README has its own code block.
- `gradle check` runs ktlint on the root `build.gradle.kts` and `settings.gradle.kts` and on the included build
  `build-logic`, in addition to the subprojects. ktlint found four formatting violations in
  `java-conventions.gradle.kts`, which are fixed.
- Coffee Kitty uses Gradle 9.9 instead of 9.8 (`mise.toml`), and `mise install` installs it. In IntelliJ IDEA, select
  the `gradle-9.9.x` directory as the local Gradle installation (README, section IntelliJ IDEA).
- For a failed test, Gradle prints the assertion message, e.g., `expected: 200 but was: 404`, and the stack trace with
  the class and method of each line, which IntelliJ IDEA links to the source.
- The workflow `.github/workflows/mise-outdated.yml` checks the pins in `mise.toml` every Monday and opens an issue when
  a pin no longer covers the newest release of its tool, because Dependabot does not read `mise.toml`.
- When IntelliJ IDEA runs a single scenario of a feature file, Cucumber loads the step definitions from the package of
  `CucumberTests` (`application/src/test/resources/cucumber.properties`) and no longer scans every test class.

### Fixed

- A successful build no longer ends with the deprecation notice that detekt 2.0.0-alpha.6 causes. Until a detekt
  release includes the fix from detekt/detekt#9577, `org.gradle.warning.mode=none` in `gradle.properties` hides all
  Gradle deprecation warnings.
- IntelliJ could not start the Gradle daemon when it ran Gradle on a JDK older than 23, because such a JDK rejects the
  option `--sun-misc-unsafe-memory-access=allow` in `org.gradle.jvmargs`. `gradle/gradle-daemon-jvm.properties` now
  requires JDK 25 for the daemon. When Gradle finds no JDK 25 on the machine, it downloads a JDK 25 from the URLs in
  this file. The foojay toolchain resolver in `settings.gradle.kts` looked up these URLs.

## [0.1.0] - 2026-10-31

### Added

- A REST API that lists and creates users, adds a coffee to a user's coffee count, and stores the users and their
  coffee counts in PostgreSQL. A multi-module Gradle build runs ktlint, detekt, and the tests.

[0.2.3]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.3
[0.2.2]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.2
[0.2.1]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.1
[0.2.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.2.0
[0.1.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.0
