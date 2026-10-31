# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-10-31

### Changed

- Spring Boot 4.1.2, which manages Tomcat 11.0.27 and Jackson 3.1.7 and 2.21.7, so `gradle.properties` no longer
  overrides those versions.
- The tests run on Testcontainers 2.0.5, whose version the Spring Boot BOM manages, and on the JUnit release of the
  version catalog (6.1.3) instead of the one that the Spring Boot BOM manages.
- Gradle 9.8, Kotlin 2.4.21, ktlint 1.8.0 with its Gradle plugin 14.2.0, Cucumber 8.0.4, springdoc 3.2.0,
  mise-action 5.2.4, and upload-artifact 7.0.2.
- detekt and ktlint run on the Kotlin 2.4.10 compiler (`detekt-kotlin` and `ktlint-kotlin` in the version catalog), so
  Dependabot proposes Kotlin updates again.

### Fixed

- The Mockito agent argument of the test tasks no longer keeps the Gradle configuration cache from storing them.

## [0.1.0] - 2026-10-02

### Added

- A REST API that lists and creates users and adds a coffee to a user's coffee count, with a PostgreSQL database and a
  multi-module Gradle build that runs ktlint, detekt, and the tests.

[0.1.1]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.1
[0.1.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.0
