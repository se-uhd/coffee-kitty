---
paths:
  - "**/src/test/**"
  - "**/src/testFixtures/**"
---

# Tests

## Test levels

Test each part of the code at the level that can detect its faults:

- Controllers have no unit tests. The system and acceptance tests check them.
- Write unit tests for code with logic of its own: the domain module, helper functions, and a mapper with significant
  logic. The system tests check a mapper that only copies properties. The unit test of a class that depends on ports
  replaces the ports with Mockito mocks. The test of a mapper creates the implementation that MapStruct generates, such
  as `UserDtoMapperImpl`, with its constructor.
- Write integration tests in `data/src/test/` for the behavior that only the database can show, such as constraints,
  queries, locks, and `ON DELETE` actions. Do not repeat what a system test checks. A table without an endpoint has
  only its integration tests. An integration test class extends `AbstractDataIntegrationTest`, which runs it against
  PostgreSQL in Testcontainers.
- A system test class in `application/src/test/kotlin/de/seuhd/campuscoffee/tests/system/` extends
  `AbstractSystemTest`.
- An acceptance test is a Cucumber scenario in a feature file in
  `application/src/test/resources/de/seuhd/campuscoffee/tests/acceptance/`, with its step definitions in the package
  `tests/acceptance/`. `CucumberSpringConfiguration` replaces the `Clock` bean with a `SettableClock`, which the steps
  set to the times of the scenario. A step names a user by login name and looks up the id with
  `ConsumptionRequests.userId`.

## Requests of the system and acceptance tests

- Send every request through `UserRequests` and `ConsumptionRequests`, and never build an HTTP request in a test, so
  that a reader of a test does not need to know the REST API. Only `ErrorResponseSystemTests` builds its requests,
  because the request itself is under test.
- Use no class of the api module, such as a DTO (ADR-007). A request body is a map with the JSON field names, such as
  `mapOf("delta" to 1)`. Parse a response into a class of `Json.kt`, such as `UserJson`, and a timestamp with
  `parseUtc`. When an endpoint gets a new field, add the field to its class in `Json.kt`.
- Write the names of the header `X-Acting-User` and of its challenge as literals, as `SystemTestUtils` does.

## Test fixtures

- Take a test object from `TestFixtures` when a test needs one of its objects. Create the object in the test class when
  the test needs values that these objects do not have, such as an id and timestamps.
- Prepare the state that each test of a class needs, such as a stored user, in a function with `@BeforeEach` (see
  `CoffeeConsumptionDataServiceIntegrationTest.storeUser`).
- Inject `DatabaseCleaner` and call `clean`, so that only the data module knows how the app reaches the database.
- Other modules add the test fixtures with `testImplementation(testFixtures(project(":domain")))`. Keep code that
  deletes data in `src/testFixtures`, never in `src/main` (ADR-005).

## What a test checks

- Check behavior that a caller can observe, such as the status, the error code, the exception type, or the stored
  state. Never check an implementation detail, that is, anything that can change while the behavior stays the same,
  such as the wording of a message, an annotation, a configuration setting, the calls between domain classes, or the
  number and order of calls.
- Do not check a framework either, such as the messages of Jackson or the rule of Spring that a lock needs a
  transaction.
- A test may check which values a message contains or leaves out, such as the name of an invalid field, a rejected
  value, or a login name in a log message.
- The tests in `models/` are the exception, because they check the assumptions of a model about the code
  (`.claude/rules/models.md`).
- Do not write a test only to reach a coverage minimum.

## Test names

- Name a test function (`@Test` or `@ParameterizedTest`) with a Kotlin backtick name that reads as a sentence in active
  voice and present tense. The subject and the verb come first, and the condition follows.
- The name contains one outcome that the test asserts, such as an HTTP status, an exception type, or a returned value.
  Do not add that a refused operation changes nothing, although the test asserts it.
- Avoid `should`, and use a verb instead of a noun (`deleting a user`, not `delete of a user`).
- Examples are ``fun `undo throws ConflictException for a coffee added one hour ago`()`` and
  ``fun `posting a delta of 2 returns 400 Bad Request`()``. Other functions have camelCase names.
