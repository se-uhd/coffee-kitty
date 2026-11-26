---
description: "the steps to add an entity across the modules."
paths:
  - "domain/src/main/kotlin/de/seuhd/campuscoffee/domain/model/**"
  - "domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/**"
  - "data/src/main/resources/db/migration/**"
  - "data/src/main/kotlin/de/seuhd/campuscoffee/data/persistence/**"
---

# Adding an entity

Follow these steps in this order. `gradle build` passes after each step. The ports come first, because a port without
an implementation does not break the build. The data adapter (step 7) comes before the domain service (step 8),
because a `@Service` needs a bean for each port that it uses. For the tables of later features, steps 4 and 5 are done.
For the price per cup, the payments, the beans, and the ratings, steps 1, 3, 6, and 7 are done as well, and the next
step is the API port (`.claude/rules/later-features.md`).

1. Create the domain model in `domain/.../model/`. It implements `DomainModel<UUID>` and has a nullable `id`,
   `createdAt`, and `updatedAt` (see `User`). Declare each rule for a value as a constraint with a message. Before the
   change goes to `main` of se-uhd/coffee-kitty, add the model to the Alloy model (`.claude/rules/domain.md`).
2. Create the API port in `domain/.../ports/api/`. An operation that only the startup task or the test setup calls
   belongs to an internal port in `domain/.../ports/internal/`.
3. Create the data port in `domain/.../ports/data/`. `.claude/rules/domain.md` states when it extends
   `CrudDataService<DOMAIN, UUID>`.
4. In one step, create the Flyway migration, the JPA entity in `data/.../persistence/entities/`, and a
   `ConstraintMapping.REGISTRY` entry for each unique constraint, because Hibernate checks every JPA entity against the
   schema at startup. Choose the `ON DELETE` action of each foreign key (`.claude/rules/data.md`).
5. Create the repository in `data/.../persistence/repositories/` (extend `JpaRepository`).
6. Create the entity mapper in `data/.../mapper/` and map each reference to another entity by its id
   (`.claude/rules/data.md`).
7. Create the data adapter in `data/.../implementation/` (extend `CrudDataServiceImpl`).
8. Create the domain service, the implementation of the API port, in `domain/.../implementation/`. Call
   `DomainValidator.requireValid` with a new domain object before the data port stores it.
9. Create the DTOs in `api/.../dtos/` and the DTO mapper in `api/.../mapper/` (`.claude/rules/api.md`).
10. Create the controller in `api/.../controller/` and keep it thin. An endpoint that creates an object returns 201
    Created with a `Location` header. The endpoint needs an acting user unless every client may use it.
11. Add tests: a domain unit test modeled on `UserServiceTest`, an integration test class for the data adapter, such as
    `PaymentDataServiceIntegrationTest`, and a system test class that extends `AbstractSystemTest`. The integration
    tests include an update without timestamps, which fails when the entity mapper copies the timestamps. Add the
    functions for the requests to a class such as `UserRequests` and the JSON bodies to `Json.kt`.
12. Add the new terms to `GLOSSARY.md`, and describe the new endpoints with `@Operation` for Swagger UI. Update the
    texts that summarize the entities, such as the introduction of `README.md`, the description in `OpenApiConfig`,
    and the glossary entry "domain exception".
