# Should the code be split into Gradle modules, and how should the modules share their build configuration?

## Decision Metadata

### Status

Accepted

### Date

2026-09-30

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The teaching core of CampusCoffeeConsumption, a minimal coffee counter, follows a hexagonal (ports-and-adapters)
architecture. The domain holds the models, the ports, and the business logic. A REST adapter calls the domain's API
ports, and a Jakarta Persistence (JPA) adapter implements its data ports. A Spring Boot application module wires the
adapters and the domain together. Each adapter may use only the domain, and the domain may use none of the other
parts. The application module may use all three. ArchUnit tests check these rules on the compiled classes, but only
when the tests run.

The parts need mostly the same build setup: the Java 25 toolchain, the Kotlin compiler options, the Spring Boot bill of
materials (BOM), the test dependencies and the arguments of the test Java virtual machine (JVM), ktlint, and detekt
with the custom KDoc rules. Some parts need more, such as the Kotlin JPA plugin for the JPA adapter and kapt (the
Kotlin annotation processing tool) for the MapStruct mappers of both adapters. The core is also the running example of
a lecture, and the lecture's first unit covers build automation with Gradle. The full CampusCoffeeConsumption
application, from which the core is ported, builds with Gradle as well.

## Considered Options

* A single Gradle project with one package per layer. The build is the simplest, but every class compiles against one
  classpath. The domain code can then import JPA and Spring MVC (model-view-controller) classes, and only the ArchUnit
  tests catch an import across layers.
* A multi-module build that shares the setup through `subprojects {}` or `allprojects {}` blocks in the root build
  script. The module graph turns an undeclared dependency between modules into a compile error, and the shared setup is
  in one file. A block applies to every project that it matches, so a setup for only some modules needs a filter by
  project name, and a subproject's build script does not show what the root script adds to it.
  [Gradle's documentation](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html)
  calls this cross-project configuration an improper way to share build logic and recommends convention plugins, in
  `buildSrc` or in an included build named `build-logic`, instead.
* A multi-module build with an included build, `build-logic/`, that holds precompiled script plugins (convention
  plugins) and imports the same version catalog. The module graph enforces the layers as in the second option, and
  each module applies only the conventions that it needs. `build-logic/` is a second build. Its dependencies must name
  the jar of every Gradle plugin that a convention applies.
* Maven with a parent POM (Project Object Model) and one module per layer. The module graph enforces the layers as
  well, and the parent POM shares the setup through inheritance. Maven would replace the build tool that the lecture
  teaches and that the full application uses.

## Decision(s)

The build is a multi-module build with the convention plugins in the included build `build-logic/` (the third option).
The module graph turns a dependency across layers into a compile error before any test runs, which the single project
cannot do. Each module names the conventions that it applies in its own `plugins {}` block, where a `subprojects {}`
block would configure the module from the root script, with nothing in the module's own build file to show it. Unlike
the Maven option, the build stays on Gradle, the build tool that the lecture teaches.

`build-logic/` holds these convention plugins, each applied by the id `de.seuhd.campuscoffee.<name>`:

* `java-conventions`: the Java toolchain, the Spring Boot BOM, the shared test dependencies, and the test JVM
  arguments.
* `kotlin-conventions`: the Kotlin compiler options, the Spring all-open plugin, ktlint, and detekt with the KDoc
  rules.
* `kotlin-jpa-conventions`: the Kotlin JPA plugin, which only the data module applies.
* `kotlin-kapt-conventions`: kapt for MapStruct with `mapstruct.unmappedTargetPolicy=ERROR`, which the api and data
  modules apply.
* `detekt-rules-conventions`: the setup of the `detekt-rules` subproject, which runs its own rules on its sources.

`gradle/libs.versions.toml` is the version catalog of both builds, and its `java` entry is the single source of the
Java version for the toolchain and the Kotlin target.

## Consequences

* A class that imports from a module that its own module does not depend on fails to compile. The ArchUnit tests still
  check the rules that the module graph cannot express, such as the rule that no controller depends on a data port.
* A module's build file lists only the conventions that it applies and its own dependencies, so a reader opens the
  convention plugins in `build-logic/` to see the module's full setup.
* A change to the shared setup is made once, in `build-logic/`, and takes effect in every module that applies the
  convention.
* `build-logic/build.gradle.kts` lists the jar of every Gradle plugin that a convention applies, and
  `build-logic/settings.gradle.kts` imports the version catalog from `../gradle/libs.versions.toml`. Both add Gradle's
  own repository, because the Gradle plugin of detekt 2.0 needs an artifact that only that repository publishes.
* Gradle compiles `build-logic/` before the first build and after each change to it.
* The root `build.gradle.kts` declares the Kotlin Gradle plugin with `apply false`, so that one classloader provides it
  to every convention plugin.
