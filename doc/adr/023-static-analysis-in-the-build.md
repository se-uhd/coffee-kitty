# Which static analysis does `gradle build` run?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Until 2026-11-26, `gradle build` ran ktlint on every Kotlin file, but detekt applied its rules only to `src/main` of the
modules. `gradle detektComments` applied only the comment rules of ADR 013 to the tests, the test fixtures,
`build-logic`, and the Gradle scripts, and a warning of the Kotlin compiler did not fail the build. The coffee app of
SE@UHD had the same gaps until its Qodana scan found 28 problems in code that had passed its build, such as a redundant
`Unit`, an unused import, and a redundant escape in a regular expression. Coffee Kitty shares some of these files, such
as the Markdown checks of `build-logic` and the models. Students run `gradle build` on their own machines, and their
tests and the changes of an agent get the same checks as the code of `main`.

## Considered Options

* Keep detekt on `src/main` and the comment rules on the other files.
* Fail on compiler warnings, apply all detekt rules to every Kotlin file, and add the detekt rules with type resolution
  that match an inspection of Qodana.

## Decision(s)

`gradle build` runs the stricter checks (the second option):

* The Kotlin compiler fails on a warning in every compilation (`allWarningsAsErrors` in the convention plugins and in
  `build-logic`) and in every Gradle script (`org.gradle.kotlin.dsl.allWarningsAsErrors` in `gradle.properties` and in
  `build-logic/gradle.properties`).
* `gradle detekt` of each module applies all rules of detekt with `config/detekt/detekt.yml` to `src/main`, `src/test`,
  and `src/testFixtures`. `gradle detektBuildLogic` applies the same rules to `build-logic` and the Gradle scripts and
  replaces `detektComments`. `detekt.yml` also activates `UseIfInsteadOfWhen`, which matches an inspection of Qodana.
* `detektMain`, `detektTest`, and `detektTestFixtures` of each module apply the rules of
  `config/detekt/type-resolution.yml` with type resolution, and `gradle check` runs them. These rules report nothing
  without type resolution and match an inspection of Qodana: `OptionalUnit`, `UnnecessaryFullyQualifiedName`,
  `UnnecessaryLet`, `UnusedImport`, `UseOrEmpty`, and `VarCouldBeVal`, which skips a `lateinit var` and a field with
  `@LocalServerPort`, because Spring sets both after the construction.
* `application` builds no plain jar, which the Kotlin plugin names as a friend path of the tests, so its `detektTest`
  takes the classes of the main source set instead and prints no warning about the missing jar.

## Consequences

* On 2026-11-26, the compiler found 2 warnings, an unused `Unit` in `ChangelogStructure`. detekt found 5 problems in the
  tests and `build-logic`, 3 of them in `ChangelogStructure.check`, which is now split into functions, and with type
  resolution 10 more, 7 of them unused imports. The fixes of the coffee app of SE@UHD for the shared files are applied.
* The tests and the solutions of the assignments get the same rules as the production code, with the defaults of detekt
  for tests, such as no `MagicNumber` and no `TooManyFunctions` in `src/test`.
* The other rules that need type resolution stay inactive.
* `models` is a module only with `-Pmodels`, so `gradle build` checks its sources only with `-Pmodels`.
