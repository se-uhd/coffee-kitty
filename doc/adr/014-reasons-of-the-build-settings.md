# Why do the build scripts set these options?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

ADR 013 allows a comment in a build script only as a pointer to an ADR or as a link to an external constraint. The
comments that gave the reasons for the settings below were neither, and no other file stated these reasons.

## Considered Options

* Keep the reasons as comments next to the settings.
* Record the reasons in one ADR and point to it from each setting.

## Decision(s)

Each setting below has the comment `// ADR-014`, and this ADR gives its reason (the second option).

* `-Xshare:off` for the test JVMs (`java-conventions`, `pitest-conventions`): The Mockito agent appends to the boot
  class path, and with class data sharing on, the JVM then prints "Sharing is only supported for boot loader classes".
* `exceptionFormat = FULL` (`java-conventions`): For a failed test, Gradle prints the assertion message and a stack
  trace with the class and method of each line, which IntelliJ IDEA links to the source.
* `javaParameters` (`kotlin-conventions`): The class files keep the parameter names, by which Spring binds the
  parameters with `@PathVariable` and `@RequestParam`.
* `kotlin("plugin.spring")` (`kotlin-conventions`): The plugin opens the classes with `@Component` or `@Transactional`,
  so that Spring can create proxies of them.
* The Kotlin version of ktlint (`kotlin-conventions`, `detekt-rules-conventions`): Without the block, the Spring
  dependency management resolves the Kotlin artifacts of ktlint to the `kotlin` version of the build, on which ktlint
  1.8.0 fails. Delete the block when `gradle ktlintCheck` passes without it.
* `mapstruct.unmappedTargetPolicy` `ERROR` (`kotlin-kapt-conventions`): A mapper fails to compile when a target
  property has neither a source nor `ignore = true`. With the default warning, a mapper would set a new property of a
  domain model to null without notice, and an update would store the null.
* `detekt-rules-conventions` for `detekt-rules`: `kotlin-conventions` adds `detektPlugins(project(":detekt-rules"))`,
  which would make the subproject depend on itself.
* The Spring Boot BOM in `coverage`: The plugin `jacoco-report-aggregation` resolves the dependencies of the modules,
  and the BOM gives their versions.
* The data module and the PostgreSQL driver as compile dependencies of `application`: No source imports them, but
  IntelliJ IDEA resolves the keys of `application.yaml`, such as `driver-class-name`, only from compile dependencies.
* The glue hint of Cucumber off (`CucumberTests`): The hint only logs a suggestion, and it fails with
  `IncompatibleClassChangeError` on a class that Kotlin generates for an inlined reified call, such as `returnResult`.

## Consequences

* A setting and its bullet change together. A new setting that needs a reason gets a bullet here.
