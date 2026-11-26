---
description: "the controllers, the acting user, the DTOs, and the error responses."
paths:
  - "api/**"
---

# API module

## Controllers

- Keep a controller thin: It reads the request, calls an API port, and maps the result to a DTO. Controllers have no
  unit tests, and the system and acceptance tests check them.
- `ApiWebConfig` adds the prefix `/api` to the path of every controller, so leave the prefix out of `@RequestMapping`.
- Declare `produces = [MediaType.APPLICATION_JSON_VALUE]` on the `@RequestMapping` of a controller (ADR-024). Spring
  then refuses a request whose `Accept` header excludes JSON before the controller function runs and changes data.
- An endpoint that creates an object returns 201 Created with a `Location` header (see `UserController.create`).
- Every endpoint needs an acting user (ADR-011). Put `OpenEndpoint` only on a function with an endpoint that every
  client may use, such as reading a coffee count. A function with `OpenEndpoint` must not call
  `CurrentUserProvider.currentUser`, which an architecture test checks.
- Pass the acting user from `CurrentUserProvider` to the domain, which checks the role (ADR-003).
- Write every message of the API in English.
- Describe each new endpoint with `@Operation` and the fields of its DTOs with `@Schema`, which Swagger UI shows. The
  detekt rule `OpenApiTextLength` limits a summary to one sentence of 60 characters and a description to 120.

## DTOs

- The api module checks the structure of a request body, and the domain checks its values (ADR-006).
- In a request DTO, a required field is a property that is not nullable, such as `val delta: Int`, and an optional
  field is a nullable property with the default `null`. Do not add a Bean Validation annotation such as `@NotNull` or
  `@Size` to a DTO, and do not make a required property nullable (ADR-008).
- A request DTO has no property for a value that the server assigns (see `CreateUserDto`). Its `@Schema` annotations
  document the limits of the domain in the OpenAPI definition.
- A response DTO has only non-null properties, including `id`, `createdAt`, and `updatedAt`, and marks each as required
  with `@field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)` (see `UserDto`). A value that can be absent, such as
  `undoableUntil` or the note of a correction, is null and also has `nullable = true`.
- The `spring.jackson` settings in `application.yaml` and `JsonConfig` reject an unknown field and a value of the wrong
  JSON type with a 400, so a new DTO needs no code for these checks.

## Error responses

- The body of every error response is an `ErrorResponse`. Its `errorCode` is the class name of the domain exception or,
  for any other error, the reason phrase of the status without spaces, such as `BadRequest`.
- Map each new domain exception to its status in `GlobalExceptionHandler.statusOf`.
- `ErrorResponsesCustomizer` documents 400, 401, 406, 415, and 500 on each endpoint to which their rule applies.
  Declare the statuses of the domain exceptions of an endpoint with `@ApiResponse`, such as `UserNotFoundResponse`,
  and add `@ResponseStatus` with the success status, which springdoc leaves out otherwise.
- `OpenApiDefinitionSystemTests` fails when a test receives a status that the OpenAPI definition does not document
  for the endpoint, and when no test receives a documented status.
- Code that runs before Spring MVC, such as the security filter chain, writes its `ErrorResponse` with
  `writeErrorResponse`, as `RequestRejectedExceptionHandler` and `JsonAuthenticationEntryPoint` do.
  `ErrorPageController` answers the error page `/error` with an `ErrorResponse`, and a direct request with 404.
- The API reads only JSON, so `application.yaml` turns off the form content filter and the multipart resolver, which
  parsed a body before the security filters or the lookup of the controller function.
