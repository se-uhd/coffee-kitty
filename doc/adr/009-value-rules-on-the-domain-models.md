# How does the domain check the values of its objects?

## Decision Metadata

### Status

Accepted

### Date

2026-11-25

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

[ADR 006](006-validation-of-requests.md) decided that the domain checks every value. The domain services did this with
hand-written conditions: `UserServiceImpl` had one function per property of a `User`, and
`CoffeeConsumptionServiceImpl` had the conditions for the coffee count and the note of a correction. The rules for a
`User` were therefore not at the class `User`, which held only the constants. Each new domain model needed new
conditions in a service, and nothing in the model showed which values are valid.

## Considered Options

* Hand-written conditions in the domain services (the state until this decision).
* Conditions in the constructor of the domain model. An invalid object then cannot exist. The entity mappers create
  the domain models from stored rows, so a rule that becomes stricter makes the stored data unreadable.
* Bean Validation constraints on the properties of the domain models. Spring checks them through `@Validated` on the
  domain service and `@Valid` on the parameter. Spring applies this check only to a call from another bean. It does
  not apply to `createInitialAdmin`, which calls `create` in the same class, or to a unit test that creates the service
  without Spring. A violation throws a `ConstraintViolationException` of Jakarta, which is not a domain exception.
* Bean Validation constraints on the properties of the domain models. The domain service checks the object with a
  `Validator` before it stores the object.

## Decision(s)

A domain model declares each rule for a value as a Bean Validation constraint with a message on the property (the last
option). A domain service calls `DomainValidator.requireValid` with the domain object before it stores the object.
`DomainValidator` throws a `ValidationException` with the messages of the violated constraints. The api module still
checks only the structure of a request ([ADR 008](008-required-fields-of-a-request.md)).

## Consequences

* The rules for a domain object are at its class, in a declarative form. A new domain model needs no new condition in
  a service.
* The check applies to every caller of a domain service, including the bootstrap admin loader and the test setup, and
  the unit tests of the domain services execute it without Spring.
* The domain compiles against the API of Jakarta Bean Validation. This dependency changes the statement of
  [ADR 002](002-spring-annotations-in-the-domain-module.md) that the domain cannot import Bean Validation. Hibernate
  Validator is only on the runtime classpath, so the domain cannot use a class of Hibernate Validator.
* Every constraint has a message of its own, because Hibernate Validator translates its default messages to the locale
  of the JVM.
* `correctCount` creates the `CountCorrection` after it has locked the consumption and then checks it. A request with an
  invalid value for an unknown user gets a 404 instead of a 400, and an invalid request locks the consumption until its
  transaction ends.
* A rule for a parameter of an operation that is not a property of a domain model stays a condition in the domain
  service. The only such rule is the delta of `applyDelta`, which must be `+1`.
* The request DTOs repeat the limits of the domain models in `@Schema` annotations, which only document them.
