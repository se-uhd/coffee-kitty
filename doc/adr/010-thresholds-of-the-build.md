# Which thresholds does the build use?

## Decision Metadata

### Status

Accepted

### Date

2026-11-25

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The build fails at several thresholds: a size of a duplicate, a mutation score, two coverage ratios, and a line length.
Other numbers set the memory of the Gradle daemon and the wait times of tests. A review found that the repository gave
no reason for most of these numbers. Some of the numbers came from the coffee app of SE@UHD, which is larger. For other
numbers, only a commit message recorded the measurement, or nobody recorded it. Nobody can check a threshold without a
recorded measurement, and nobody knows when to change it.

The measurements below ran on 2026-11-25 on one developer machine (16 cores, macOS, JDK 25, Gradle 9.9.2) unless a row
names another date. No measurement ran on the CI runner.

## Considered Options

* Keep the numbers and add the reasons that the coffee app of SE@UHD gives.
* Measure each number on Coffee Kitty, keep the numbers that the measurement supports, remove the settings that show no
  effect, and record the measurement next to the number.

## Decision(s)

Every threshold has a measurement on Coffee Kitty (the second option). A comment at the threshold states the result and
the date, and this ADR has the numbers.

### Duplicates: 70 tokens (`build.gradle.kts`)

Number of duplicates that CPD reports in the production code for each limit, measured on 2026-11-23 and 2026-11-25 with
the same result from 40 tokens up:

| Limit in tokens | 30 | 40 | 50 | 60 | 61 | 70 | 80 | 100 |
|---|---|---|---|---|---|---|---|---|
| Duplicates | 13 | 6 | 2 | 1 | 0 | 0 | 0 | 0 |

The largest duplicate has 60 tokens in 6 lines: the `@Mapping` annotations that `CoffeeConsumptionEntityMapper` and
`PaymentEntityMapper` share. MapStruct needs these annotations at each function, so the build cannot extract them, and
the limit has to be above 60. The second largest has 53 tokens in 8 lines, in `RequestRejectedExceptionHandler` and
`SecurityConfig`.

A second measurement shows how large a copy of real code has to be before CPD reports it. The measurement ran CPD on
copies of the first lines of three domain functions. At 70 tokens, CPD reports the copy at 10 to 16 source lines
(`UserServiceImpl.create` 16, `undo` 10, `correctCount` 15). At 100 tokens, it reports the copy only at 13 to 17 lines
(measured on 2026-11-23). The limit is 70, the lowest multiple of ten above the largest duplicate that cannot be
extracted.

### Mutation testing: 90% (`pitest-conventions.gradle.kts`)

The unit tests detect 31 of the 32 mutants in `domain/.../implementation/` (96.9%). The surviving
mutant is a condition in the inlined code of `any` in `createInitialAdmin`, which does not change the behavior. The
mutation score and the test strength have the same value, because the tests execute every mutant. With 32 mutants, each
mutant is 3.1 points, so a minimum of 90% lets the task pass with up to three undetected mutants and fail at the fourth.

Two settings from the coffee app of SE@UHD are gone, because they showed no effect in 15 runs:

* `timeoutConstInMillis` 30,000: No mutant timed out with this value or with the PIT default of 4,000, and the results
  were identical. The slowest unit test takes 82 ms.
* `threads` 1: The value is the PIT default.

### Coverage: 90% of the lines and 80% of the branches (`coverage/build.gradle.kts`)

With the exclusions of the gate, the tests cover 695 of 715 lines (97.2%) and 142 of 166 branches (85.5%). 8 of the 24
missed branches belong to the three functions of the undo status, which have no tests until 2026-12-08. With these
tests, the tests cover 150 of 166 branches (90.4%).

The minimums are on purpose 7.2 and 5.5 points below the measured values. A minimum close to the measured value makes
developers write tests only to reach the number. Such tests check implementation details and are more than the code
needs, which the testing strategy of the project rules out. The minimums were 95% and 85% from 2026-11-24 to 2026-11-25.
A commit of 2026-11-24 raised them, because the ratio had not detected that the undo status had no test. The second rule
of the gate, which requires a test for every function of a domain service and of a controller, detects that case, so the
higher minimums added only the pressure described above.

### Memory of the Gradle daemon: 2g of heap and 768m of metaspace (`gradle.properties`)

A full build without the caches (`gradle cleanTest build --no-build-cache --rerun-tasks` without the configuration
cache), each from a stopped daemon:

| Setting | Runs | Passed | Peak heap in MB | Peak metaspace in MB |
|---|---|---|---|---|
| Heap 2g, metaspace 768m (the setting) | 4 | 4 | 370 to 795 | 291 to 301 |
| Gradle defaults: heap 512m, metaspace 384m | 5 | 5 | 359 to 446 | 291 to 298 |
| Heap 1g, metaspace 768m | 4 | 4 | 357 to 484 | 291 to 296 |

No run failed with an `OutOfMemoryError`, so the measurement shows no need for the heap of 2g on this machine. The heap
stays at 2g to be conservative. With the default, a build used up to 87% of the heap. Nobody measured the build on the
CI runner or on the machines of students, and every CI build so far ran with 2g. The metaspace differs: One such build
uses about 300 MB, which is 78% of the default. With the default, the daemon printed "The Daemon will expire after the
build after running out of JVM Metaspace" in the third build in a row. With 768m, ten builds in a row printed no
warning, and the metaspace grew by about 16 MB per build.

### Line length: 120 characters (`.editorconfig`)

120 is the default right margin of IntelliJ IDEA. Without the setting, the code style `ktlint_official` allows 140.

### Wait times of tests

In 34 full builds, a whole concurrency test in `ConsumptionSystemTests` took at most 0.6 seconds, against a limit of 30
seconds for each request. In the lock test of `CrudDataServiceIntegrationTest`, the steps other than the deliberate wait
took at most 35 ms. The wait of 500 ms is therefore 14 times as long as the second transaction would need without the
lock. Only a step that hangs reaches the limit of 10 seconds.

## Consequences

* Each threshold can be checked: Repeat the measurement, and compare the result with the comment.
* Before someone changes a threshold or adds one, they measure and record the result and the date at the threshold. A
  changed decision gets a new ADR.
* The coverage minimums do not rise with the coverage. A function without a test fails the second rule of the gate,
  not the ratio.
* The measurements are from one fast machine. Before someone lowers the heap, they measure the build on the CI runner.
* PIT runs with its default timeout. If a slower machine reports a mutant as timed out, raise the constant and record
  the measurement.
