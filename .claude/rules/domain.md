---
description: "the ports, the value rules, the locks, and the models that follow a domain model."
paths:
  - "domain/**"
---

# Domain module

## Ports and domain services

- The controllers call the API ports in `ports/api/`, and the data module implements the data ports in `ports/data/`.
  The startup task, the test setup, and the dev application act without an acting user and call the internal ports in
  `ports/internal/`.
- The port packages hold only interfaces named `*Service`, and `ports/data/` only interfaces named `*DataService`.
- The data port of an entity that the domain changes and deletes extends `CrudDataService<DOMAIN, UUID>`. The data port
  of an entity that never changes after it is stored declares only the operations that the domain needs, as
  `CountCorrectionDataService` does with `insert` and `getAllByUserId`.
- Implement the API ports and the internal ports in `domain/.../implementation/`.
- Check the role of the acting user in the domain service with `requireAdmin` (ADR-003), not in the controller.
- A function that changes a consumption runs in a transaction and reads the consumption with `getByUserIdForUpdate`,
  which locks the row until the transaction ends. A second request for the same user then waits and reads the changed
  count.

## Value rules

- Declare each rule for a value of a domain model, such as a length or a range, as a Bean Validation constraint on the
  property, such as `@field:Size` on `User.loginName` (ADR-009).
- Give every constraint a `message`, because Hibernate Validator translates a default message to the locale of the
  JVM. Constraints on one property that state one rule share a message constant.
- Call `DomainValidator.requireValid` with a domain object before a data port stores it. A violated constraint becomes
  a `ValidationException` (400), so the rules also hold for an object that no request creates, such as the bootstrap
  admin.
- Do not use `@Validated` with `@Valid` on a domain service, because Spring applies the check only to a call from
  another bean and not in a unit test.
- Check a rule for a parameter that is not a property of a domain model in the domain service, such as the delta of
  `applyDelta`.
- Check every rule before you call a data port. A `DataConstraintException` from a data port is not a domain exception
  and becomes a 500.
- To add a domain exception, subclass the sealed class `DomainException` and add its status to
  `GlobalExceptionHandler.statusOf` in the api module.

## Models of the domain

The tests in `models/` check two models against this module (`.claude/rules/models.md`).

- `ConsumptionConformanceTest` replays `models/src/main/tla/Consumption.tla` on `CoffeeConsumptionServiceImpl`. When you
  change the behavior of the service or a function of its port, change the model with it.
- When you add or change a property of a domain model, change the Alloy model in the same step:
  1. In `models/src/main/alloy/DomainModel.als`, give the sig of the class a field for each property except `id`,
     `createdAt`, and `updatedAt`, and a predicate `valid<Sig><Field>` for each String and Int property. Add the
     predicate of each String property to the assertion `NoTextPropertyAcceptsNul`.
  2. Add the concrete values of each String and Int property to `TEXTS` or `NUMBERS` in `DomainModelConformanceTest`,
     which fails until the model and the class match.
  3. Run `gradle :models:classDiagrams` and commit the class diagram.

## Mutation tests

Run `gradle :domain:pitest` before you push a change to this module. For each surviving mutant that changes the
behavior, add a test or improve an assertion. The report is in `domain/build/reports/pitest/index.html`. The mutant of
a condition in the inlined code of `any` in `createInitialAdmin` survives and does not change the behavior.
