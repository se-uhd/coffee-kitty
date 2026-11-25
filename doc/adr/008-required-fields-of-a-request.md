# How does a request DTO state which fields are required?

## Decision Metadata

### Status

Accepted

### Date

2026-11-25

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

[ADR 006](006-validation-of-requests.md) decided that the api module checks the structure of a request and the domain
checks the values. For a missing field, ADR 006 gave a request DTO nullable properties with `@NotNull`, and Bean
Validation rejected a body without the field. This rule had disadvantages:

* The type of a property did not state whether the field is required. The required `loginName: String?` and the optional
  `role: Role?` were both nullable, and only an annotation distinguished them.
* The compiler could not help. A controller converted each value with `requireNotNull`, and a controller function
  without `@Valid` passed null to the domain.
* Two libraries checked the structure. Jackson rejected a value of the wrong JSON type and an unknown field, and Bean
  Validation rejected a missing field. Each had its own exception handler and its own messages.

## Considered Options

* Nullable properties with `@NotNull`, which Bean Validation checks (ADR 006).
* Properties that are not nullable. The Kotlin module of Jackson rejects a body without such a field or with `null` as
  its value.

## Decision(s)

A required field is a property that is not nullable, such as `val delta: Int`. An optional field is a nullable property
with the default `null`, such as `val role: Role? = null`. Jackson alone checks the structure of a request body, and
the api module does not use Bean Validation. The other decisions of ADR 006 stay: The domain checks every value, and a
request that Jackson cannot read gets a 400 with the error code `BadRequest`.

## Consequences

* The type of a property states the contract, and the compiler checks every use. The controllers contain neither
  `@Valid` nor `requireNotNull`, and a DTO with a missing required field cannot exist.
* One library and one exception handler check the structure. `GlobalExceptionHandler.handleHttpMessageNotReadable`
  names the field at which Jackson failed, from the path of the Jackson exception. The name is a property name of the
  DTO. For an unknown field, the name would be client input, and the message does not contain it.
* Breaking: The message of a 400 for a missing field changes. The same message now also names a field with a value of
  the wrong JSON type, which got only "Bad Request" until now. The message does not state whether the field is missing
  or has a value that Jackson cannot read.
* An error response names only the first such field, because Jackson stops at the first error. Bean Validation named
  every missing field.
* springdoc read `@NotNull` to mark a field as required in the OpenAPI definition. A request DTO now marks each
  required field with `@Schema(requiredMode = REQUIRED)`, as a response DTO does.
* The api module has no dependency on Bean Validation, and the app has no setting for the locale of its messages.
