---
description: "the build scripts, CI, and the thresholds of coverage, mutation tests, and duplicates."
paths:
  - "**/*.gradle.kts"
  - "build-logic/**"
  - "gradle/**"
  - "gradle.properties"
  - "mise.toml"
  - ".github/**"
  - "config/**"
  - "detekt-rules/**"
  - "coverage/**"
  - "scripts/**"
---

# Build

## Gradle

- A subproject applies a convention plugin of `build-logic/` by its id `de.seuhd.campuscoffee.<name>` (ADR-001). The
  root build and `build-logic/` read the same version catalog, `gradle/libs.versions.toml`.
- Declare every Maven repository in `settings.gradle.kts` or `build-logic/settings.gradle.kts`. `FAIL_ON_PROJECT_REPOS`
  fails the build when a build script declares a repository. IntelliJ IDEA marks `repositoriesMode` and `repositories`
  as unstable, because Gradle annotates them with `@Incubating`, and Gradle has no stable alternative.
- The `java` entry of the version catalog sets the Java toolchain and the Kotlin `jvmTarget`, and `mise.toml` pins the
  same major version. No tool copies the version from one file to the other, so change both files together.
- The root `gradle.properties` turns on the configuration cache. A task action must not reference a top-level value of
  its build script or a `Configuration`. Copy such a value into a local `val` first, and wrap a `Configuration` in
  `files(...)`.
- `org.gradle.warning.mode=none` in `gradle.properties` hides every deprecation warning of Gradle, also the warnings
  about the build scripts of this project. `gradle build --warning-mode all` shows them.
- `application` depends on `data` at compile scope, not `runtimeOnly`, so that the IDE resolves the configuration keys
  of the `*Properties` classes in the data module.
- detekt and ktlint run on their own Kotlin compilers, which the catalog entries `detekt-kotlin` and `ktlint-kotlin` set
  independently of the entry `kotlin`. When you update detekt, set `detekt-kotlin` to the Kotlin version with which the
  new detekt release was built. On a mismatch, detekt reports "detekt was compiled with Kotlin X".
- `gradle :detekt-rules:test` runs the tests of the custom detekt rules. `config/detekt/detekt.yml` enables them.

## CI

- `.github/workflows/build.yml` checks the commit messages, runs `scripts/check-version-sync.sh`, `gradle build`, and
  then `gradle :domain:pitest` on every push to `main` and on every pull request.
- Pin every action of a workflow (`uses:`) to a full commit SHA with a trailing `# vX.Y.Z` comment.

## Thresholds

`doc/adr/010-thresholds-of-the-build.md` records the measurement of each threshold with its date. `CLAUDE.md` states
how to change a threshold. Mark a threshold in a build script with `// ADR-010`.

- Coverage (`coverage/build.gradle.kts`): The task `coverageGate`, which `check` runs, fails when the tests cover less
  than 90% of the lines or 80% of the branches of all modules together. A ratio over all modules cannot detect a single
  function without a test, so a second rule requires that a test executes every function of a domain service and of a
  controller.
- The coverage minimums are several points below the measured coverage on purpose, so that nobody writes a test only to
  reach a number. Do not raise the minimums when the coverage grows, and do not lower them to make a build pass.
- Mutation tests (`pitest-conventions.gradle.kts`): `gradle :domain:pitest` fails when the unit tests of the domain
  module detect fewer than 90% of the mutants of `domain/.../implementation/`. PIT does not remove the null checks that
  the Kotlin compiler generates (`avoidCallsTo`), because no test can detect such a mutant.
- Duplicates (`build.gradle.kts`): CPD fails at a duplicate of 70 tokens in the production code. `gradle cpd` runs it
  alone. Extract the shared code instead of raising the threshold.
