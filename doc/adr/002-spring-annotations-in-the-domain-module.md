# May the domain module depend on Spring?

## Decision Metadata

### Status

Accepted. [ADR 009](009-value-rules-on-the-domain-models.md) adds the API of Jakarta Bean Validation to the compile
classpath of the domain.

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

In a hexagonal architecture, the domain should not depend on a framework. The domain services of Coffee Kitty still need
three features of a framework. They need dependency injection for the ports. They need transactions:
`CoffeeConsumptionServiceImpl.applyDelta` locks a row until its transaction ends, and `UserServiceImpl.create` stores a
user and a consumption together. And `GracePeriod` needs the configured grace period. Today, the domain module uses
`@Service`, `@Component`, `@Transactional`, and `@ConfigurationProperties`. The code has followed this decision since
the first version, and this ADR records it.

## Considered Options

* No framework in the domain. The application module creates the domain services in `@Configuration` classes. It also
  wraps each API port in a transaction, or the domain calls a port for transactions. The domain compiles without Spring.
  The application module then needs a bean definition for every domain service, and the transaction of a function is no
  longer visible at the function.
* Spring annotations in the domain, limited by the Gradle dependencies to the Spring Boot core (`spring-context`) and
  `spring-tx`. The domain cannot import Spring MVC, Bean Validation, or JPA, because these libraries are not on its
  compile classpath.
* The Spring Boot starters in the domain. Every Spring library is then available to the domain.

## Decision(s)

The domain module uses the Spring annotations for components, transactions, and configuration properties (the second
option). A domain service states its own transaction, which is part of its behavior: Without the transaction, the lock
of `applyDelta` does not hold. The dependencies of `domain/build.gradle.kts` keep every web and persistence library out
of the domain, and the architecture tests check the direction of the dependencies. The coffee app of SE@UHD has the
same structure.

## Consequences

* The unit tests of the domain create the services with their constructors and Mockito mocks and need no Spring context.
* The domain module does not compile without Spring. Replacing Spring with another framework requires changing the
  annotations in the domain classes.
* A unit test does not check a transaction. Integration and system tests check the lock and the rollback
  (`CrudDataServiceIntegrationTest`, `FailureSystemTests`).
* A new dependency of the domain module is an architecture decision and needs an ADR.
