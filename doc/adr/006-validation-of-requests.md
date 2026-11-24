# Which module validates a request?

## Decision Metadata

### Status

Accepted

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

A request can be wrong in two ways. The server cannot read it: A field is missing or has the wrong JSON type. Or the
server can read it, and a value breaks a rule, such as a login name of 256 characters. Until this decision, Bean
Validation on the DTOs and the domain services both checked the rules for values. The two checks of the email address
accepted different addresses (`@Email` and a regular expression). The error code of a 400 depended on the module that
rejected the value: `BadRequest` for a note of 501 characters and `ValidationException` for a note with a NUL
character. The domain needed its own checks in any case, because the bootstrap admin and the fixture users reach the
domain without a request.

## Considered Options

* Bean Validation in the api module only. The domain trusts its callers. A user that no request creates is then not
  checked.
* Both modules check every rule, with the same constants. Each rule exists twice, and the two error codes remain.
* The api module checks the structure of a request, and the domain checks the values.

## Decision(s)

The api module checks the structure, and the domain checks the values (the third option). A request DTO has nullable
properties with `@NotNull` and no other constraint. Jackson rejects a value of the wrong JSON type and a field that the
DTO does not have. Such a request gets a 400 with the error code `BadRequest`. A domain service checks every rule for a
value and throws a `ValidationException`, which becomes a 400 with the error code `ValidationException`. `UserDto` is
split: `CreateUserDto` is the request body, and `UserDto` is the response body with non-null properties.

## Consequences

* Each rule has one place, and the two error codes of a 400 have distinct meanings.
* Breaking: The error code of a 400 changes for the requests whose values the DTOs checked until now, and a body with an
  unknown field, such as `id`, is rejected.
* A request with several invalid values reports only the first one, because a domain service throws at the first
  violation. Bean Validation reported all of them.
* The OpenAPI definition gets the limits from `@Schema` annotations on the request DTOs, which repeat the constants of
  the domain model for documentation only.
* The controllers contain no rule, and a domain exception is thrown only in the domain.
