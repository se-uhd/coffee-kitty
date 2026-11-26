# How does the OpenAPI definition document the error responses?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Until 2026-11-26, no controller function had `@ApiResponse`, so the OpenAPI definition listed only the success status of
each endpoint, although the requirements, the exception handler, and the system tests state the error statuses.
Annotations alone can drift from the behavior without a test that notices it. A derivation alone cannot know which
exceptions of the domain an endpoint throws.

## Considered Options

* Declare every status with `@ApiResponse` on each controller function.
* Derive every status from the code, such as from the exceptions that a domain service declares.
* Derive the statuses that follow from a rule for all endpoints, declare the statuses of the domain, and check both
  against the statuses that the tests receive.

## Decision(s)

The API uses the third option:

* `ErrorResponsesCustomizer` adds 400, 401, and 500 to every endpoint, 406 to every endpoint with a response body, and
  415 to every endpoint with a request body, each with the schema `ErrorResponse`. The description of 401 depends on
  `OpenEndpoint`, which `SecurityConfig` also reads.
* The controller functions declare 403, 404, and 409 with `@ApiResponse`, or with the annotations `UserNotFoundResponse`
  and `NotAdminResponse` for the repeated ones. A function with `@ApiResponse` also has `@ResponseStatus`, because
  springdoc otherwise drops its success status.
* The client of the system tests records the method, the path, and the status of each response (`ObservedStatuses`).
  `OpenApiDefinitionSystemTests` runs last and fails when a received status is not documented, or when a documented
  status has no test and is not in its list `NOT_YET_TESTED`.

## Consequences

* A new endpoint, or a new status that a test receives, fails the build until the definition documents it.
* `NOT_YET_TESTED` holds the statuses 200 and 404 of `GET /api/users/{userId}/consumption/undo` until 2026-12-08,
  because the tests of the undo status are the solution of assignment 7 (`CLAUDE.md`).
* The definition does not document 405 for a wrong method, 404 for an unknown path, or HEAD requests, because OpenAPI
  has no operation for them.
* `OpenApiDefinitionSystemTests` fails when it runs alone, because then no other test has received a status.
