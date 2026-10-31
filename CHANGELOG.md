# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- The README describes how the group buys coffee beans from the kitty and how members declare their own coffee
  purchases as expenses, says that students first see the app in the lectures on agentic software engineering, and
  explains how to set up IntelliJ IDEA.

### Fixed

- A successful build no longer ends with the deprecation notice that detekt 2.0.0-alpha.6 causes:
  `org.gradle.warning.mode=none` in `gradle.properties` hides Gradle's deprecation warnings until a detekt release
  includes the fix (detekt/detekt#9577).
- IntelliJ could not start the Gradle daemon when it ran Gradle on a JDK older than 23, which rejects the option
  `--sun-misc-unsafe-memory-access=allow` in `org.gradle.jvmargs`. `gradle/gradle-daemon-jvm.properties` now requires
  JDK 25 for the daemon, and the foojay toolchain resolver in `settings.gradle.kts` lets Gradle download that JDK.

## [0.1.0] - 2026-10-31

### Added

- A REST API that lists and creates users and adds a coffee to a user's coffee count, with a PostgreSQL database and a
  multi-module Gradle build that runs ktlint, detekt, and the tests.

[0.1.0]: https://github.com/se-uhd/coffee-kitty/releases/tag/v0.1.0
