# How do the tests check the JSON format of the REST API?

## Decision Metadata

### Status

Accepted

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The JSON field names of the requests and responses are the contract with the clients of the API, and the README
documents them. Until this decision, the system and acceptance tests serialized their requests and parsed the responses
with the DTO classes of the api module. A renamed JSON field then changed the request of the test and the response of
the server together. All 113 tests of the application module passed when the server served the field `count` as
`coffeeCount` and read the field `delta` from `amount`. Yet the documented request `{"delta": 1}` returned 400.

## Considered Options

* Keep the DTOs in the tests and add one test per DTO that compares its JSON with a literal. The other tests still pass
  for a renamed field, and each new DTO needs its own test.
* Compare the OpenAPI definition with a stored copy. The copy changes with every documentation change, and a reviewer
  has to find the breaking changes in its diff.
* The tests use no class of the api module. They send request bodies as maps with the field names and parse the
  responses into classes of their own with a strict mapper.

## Decision(s)

The tests use no class of the api module (the third option). `Json.kt` in the test sources defines the bodies as the
tests expect them, such as `UserJson`. The tests parse a response with a mapper that fails on an unknown or a missing
field. A timestamp is a `String`, so that a timestamp with an offset fails as well. The header name `X-Acting-User` and
its challenge are literals in the tests. An architecture test fails if a class under `tests` depends on a class of the
api module. With the two renames above, 53 tests fail.

## Consequences

* The tests fail when a JSON field name, a field type, or the set of fields of a response changes.
* A new field of an endpoint requires a change to `Json.kt`. The field names exist twice on purpose: once in the DTOs
  and once in the tests.
* The request helpers take maps, so a test can send a body that no DTO can hold, such as a text for a number.
