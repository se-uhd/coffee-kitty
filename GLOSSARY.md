# Glossary

This glossary defines one term for each concept of Coffee Kitty. Use these terms in the documentation, in KDoc and
comments, and in the messages that the API returns. Identifiers such as class, field, table, and route names keep their
spelling, and the lines "API:" and "Code:" of an entry list the identifiers that differ from the term. Some entries end
with a line "Avoid:", which lists words not to use for the concept.

## Domain

- **user**: A person whose coffees the app counts. The app stores the login name, the email address, the first and the
  last name, and the role of the person.
  API: `/api/users`, `UserDto`, `userId`. Code: `User`, `UserService`, `users`.
  Avoid: member, drinker, account.
- **login name**: A user's unique name, which consists of ASCII letters, digits, and underscores and has at most 255
  characters. Two users can have names that differ only in case, such as `jane_doe` and `Jane_Doe`. `GET /api/users`
  sorts the users by this name, and the consumption endpoints return it together with the coffee count.
  API: `loginName`. Code: `User.loginName`, `login_name`.
  Avoid: login (for the name).
- **email address**: A user's unique address of the form name@domain, at most 254 characters long. Because the app trims
  the address and stores it in lowercase, `Jane@se.de` and `jane@se.de` are the same address.
  API: `emailAddress`. Code: `User.emailAddress`, `email_address`.
  Avoid: email (for the address).
- **coffee**: One cup that a user drinks. Adding a cup raises the user's coffee count by one.
  API: `POST /api/users/{userId}/consumption` with `delta` 1. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: bump (a count).
- **coffee count**: A user's total number of coffees. The number starts at zero when the app creates the user. Adding a
  coffee raises it by one, an undo lowers it by one, and a correction sets it to the count that an admin chooses.
  API: `ConsumptionDto.count`. Code: `CoffeeConsumption.count`, `coffee_consumptions.count`.
- **consumption**: The stored record that holds the coffee count of one user and the time of each recent coffee that the
  user did not undo and that no correction removed. Adding a coffee drops the times of the coffees outside their grace
  period. Every user has exactly one such record, which `UserService.create` or `UserInternalService.create` creates
  together with the user.
  API: `/api/users/{userId}/consumption`, `ConsumptionDto`. Code: `CoffeeConsumption`, `coffee_consumptions`,
  `coffeeTimes`, `coffee_times`.
- **delta**: The field in the request body of `POST /api/users/{userId}/consumption` that states the change to the
  coffee count. The domain accepts only +1, which adds one coffee, and rejects every other integer. An extension can
  accept -1 to remove a coffee without changing the format of the request body (see the KDoc of
  `CoffeeConsumptionService.applyDelta`).
  API: `ConsumptionDeltaDto.delta`, `{ "delta": 1 }`. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: single-step change.
- **undo**: Reversing the most recent coffee that a user added and did not undo, while that coffee is within the grace
  period. The undo lowers the coffee count by one. After an undo, the coffee before is the most recent coffee (FR-3 in
  `requirements/FR-3-undo-coffee.md`).
  API: `POST /api/users/{userId}/consumption/undo`. Code: `CoffeeConsumptionService.undo`.
  Avoid: cancel, coffee canceled.
- **grace period**: The time after a user adds a coffee, 5 minutes by default, in which the user can undo the coffee.
  The grace period includes the instant at which it ends: For a coffee added at 10:00:00, an undo at 10:05:00
  succeeds, and an undo at 10:05:01 returns 409 Conflict.
  Code: `GracePeriod`, `campus-coffee.consumption.grace-period`, environment variable
  `CAMPUS_COFFEE_GRACE_PERIOD`.
  Avoid: grace window.
- **undo status**: Whether a user can undo their most recent coffee now and, if so, the end of the grace period of that
  coffee. The undo status states that the user can undo a coffee exactly when an undo at the same time succeeds (FR-9
  in `requirements/FR-9-undo-status.md`).
  API: `GET /api/users/{userId}/consumption/undo`, `undoable`, `undoableUntil`. Code:
  `CoffeeConsumptionService.undoableUntil`, `GracePeriod.closesAt`, `UndoStatusDto`.
  Avoid: undo offer, cancellable.
- **correction**: An admin's change of a user's coffee count to a new count, which the admin explains in a note (FR-8 in
  `requirements/FR-8-correct-coffee-count.md`). An admin can correct a coffee count at any time, even after the grace
  period. Coffee Kitty stores each correction with the count before, the count after, the note, the admin, and the time
  of the correction. A correction to a lower count removes the most recent coffees, and an undo then applies to the most
  recent coffee that remains. A correction to a higher count adds coffees that the user cannot undo.
  API: `PUT /api/users/{userId}/consumption`, `ConsumptionCorrectionDto` (`total`, `note`),
  `GET /api/users/{userId}/consumption/corrections`, `CountCorrectionDto`. Code:
  `CoffeeConsumptionService.correctCount`, `CountCorrection`, `count_corrections`.
  Avoid: adjustment, override, reset (for a coffee count).
- **note**: The text with which an admin explains a correction, such as "Double tap on 16.11." A correction needs a note
  that is not blank and has at most 500 characters. A note can span several lines. A payment and an expense can have a
  note of at most 500 characters.
  API: `ConsumptionCorrectionDto.note`, `CountCorrectionDto.note`. Code: `CountCorrection.note`,
  `CountCorrection.MAX_NOTE_LENGTH`, `count_corrections.note`, `Payment.note`, `payments.note`, `expenses.note`.
  Avoid: comment, reason (for the field).
- **euro cents**: The unit of every amount of money in the code and in the database. Each amount is an integer, so that
  sums have no rounding errors.
  Code: `amountCents`, `balanceCents`, `amount_cents`, `balance_cents`.
- **price per cup**: The one price of a coffee in euro cents, which applies to every user and which an admin sets. Each
  coffee lowers the balance of its user by the price per cup at the time of the coffee, so a new price applies only to
  the coffees added after the change (FR-5 in `requirements/FR-5-pay-for-coffee.md`). An undo raises the balance by the
  amount by which the coffee lowered it (FR-11 in `requirements/FR-11-credit-undone-coffee.md`). Coffee Kitty stores at
  most one price, and no endpoint reads or sets it yet. No migration stores a price, so the startup has to store the
  start price.
  Code: `CoffeePrice`, `CoffeePriceDataService`, `coffee_prices`, `coffee_times.price_cents`.
- **balance**: A user's amount of money in euro cents, negative when the user owes the kitty money and positive when the
  kitty owes the user money. Coffees lower the balance, and deposits and the private shares of the user's expenses
  raise it. The table `user_balance` holds the balance of each user, but no code writes it yet.
  Code: `UserBalanceEntity`, `user_balance`.
  Avoid: member balance, account.
- **kitty**: The shared cash of the group in euro cents. The current amount is the kitty balance. Deposits raise the
  kitty, the kitty shares of expenses lower it, and kitty adjustments change it in either direction. The kitty never
  goes negative. The table `kitty_balance` holds the kitty balance in one row, but no code writes it yet.
  Code: `KittyBalanceEntity`, `kitty_balance`.
  Avoid: fund, coffee fund.
- **payment**: A deposit or a kitty adjustment, which an admin records. A coffee is not a payment, because it lowers the
  balance of the user without moving cash.
  Code: `Payment`, `PaymentDataService`, `payments`.
- **deposit**: Cash that a user pays into the kitty and that an admin records. A deposit raises the balance of the user
  and the kitty by the same amount, which is positive (FR-4 in `requirements/FR-4-record-deposit.md` and FR-10 in
  `requirements/FR-10-raise-kitty-balance.md`). No endpoint records a deposit yet.
  Code: `Payment` with a `user`.
  Avoid: settlement, top-up.
- **kitty adjustment**: A change of the kitty that an admin makes and that belongs to no user, such as the initial cash
  in the kitty. Its amount is positive or negative but not zero.
  Code: `Payment` without a `user`.
  Avoid: correction (for a change of the kitty).
- **expense**: Money that a user, the buyer, spent for the group, either a bean purchase or any other expense. FR-6 in
  `requirements/FR-6-record-own-expense.md` specifies how a user records an expense that they paid with their own money.
  So far, Coffee Kitty has only the table, the JPA entity, and the repository for expenses, without a domain model.
  Code: `ExpenseEntity`, `ExpenseEntity.buyer`, `expenses`, `buyer_user_id`.
  Avoid: outlay, purchase (except in bean purchase).
- **bean purchase**: The kind of expense for coffee beans, which refers to a bean and has a weight in grams.
  Code: `BEANS`, `ExpenseEntity.bean`, `ExpenseEntity.weightGrams`.
- **other expense**: The kind of expense for anything except beans, such as filters or milk, without a bean or a weight.
  Code: `OTHER`.
- **private share / kitty share**: The two parts of an expense, which add up to its amount. The private share raises the
  balance of the buyer, and the kitty share lowers the kitty.
  Code: `ExpenseEntity.privateAmountCents`, `ExpenseEntity.kittyAmountCents`.
- **bean**: One named kind of coffee beans, which bean purchases and ratings refer to. A live bean has a name that no
  other live bean has, ignoring case. An admin can merge a bean into another bean, and the merged bean stays as an
  inactive record that refers to the other bean.
  Code: `CoffeeBean`, `CoffeeBean.mergedIntoId`, `CoffeeBeanDataService`, `coffee_beans`.
- **rating**: The stored record of one score that a user gives a bean. A new rating of a user for the same bean does not
  replace an earlier rating, and a rating moves no money. FR-12 in `requirements/FR-12-rate-a-bean.md` specifies how a
  user rates a bean. No endpoint records a rating yet.
  Code: `CoffeeRating`, `CoffeeRatingDataService`, `coffee_ratings`.
- **score**: The number from 1 to 5 of a rating.
  Code: `CoffeeRating.value`, `coffee_ratings.value`.
  Avoid: value (in prose), rating (for the number).
- **role**: What a user may do in Coffee Kitty, either `USER` or `ADMIN`. An admin may also create users, correct coffee
  counts, and list the corrections of a user.
  API: `UserDto.role`. Code: `Role`, `User.role`, `users.role`, `requireAdmin`.
- **acting user**: The user who sends a request, identified by the login name in the header `X-Acting-User`. Because
  Coffee Kitty has no authentication yet, any client can act as any user.
  API: `X-Acting-User`, `WWW-Authenticate: X-Acting-User realm="Coffee Kitty"` in a 401. Code: `CurrentUserProvider`,
  `ActingUserAuthenticationFilter`, `ActingUserRequired`, `SecurityConfig`.
  Avoid: current user (except in the class name), caller.
- **bootstrap admin**: The admin that the app stores on startup from the keys under `campus-coffee.bootstrap-admin` when
  no admin exists. If a user has the configured login name, the app gives that user the
  role `ADMIN`. Otherwise, it creates the configured user.
  Code: `BootstrapAdminLoader`, `BootstrapAdminProperties`, `StartupDataInitializer`,
  `UserInternalService.createInitialAdmin`.
- **fixture user**: One of the five users in `TestFixtures`, such as `jane_doe`, the only admin among them. The dev
  application creates these users with a coffee count of zero on every start, and the system and acceptance tests
  create them the same way before each test.
  Code: `TestFixtures`, `DevDataSeeder`.
  Avoid: seeded user.
- **test fixture**: A test object with fixed values that several tests share, or the state that a test prepares before
  it runs the code under test. `TestFixtures` holds the shared test objects: the fixture users, a fixed time with a
  clock that shows it, and three consumptions. One consumption has no coffee, one has a recent coffee, and one has an
  old coffee. At the fixed time, the grace period of the old coffee has passed. A function with `@BeforeEach` prepares
  the state that each test of a class needs, such as a stored user. `TestFixtures` and the database cleaner are in the
  Gradle source set `src/testFixtures` of their module, which the production jar does not contain.
  Code: `TestFixtures`, `FIXED_TIME`, `fixedClock`, `consumptionWithRecentCoffee`, `DatabaseCleaner`.

## Architecture

- **module**: One of the four Gradle subprojects that hold the app code: `domain`, `api`, `data`, and `application`. The
  architecture tests treat each of these subprojects as a layer. The subprojects `coverage` and `detekt-rules` hold
  build tooling instead of app code.
- **convention plugin**: A Gradle plugin in the included build `build-logic/` that configures one part of the build,
  such as the Java toolchain or the Kotlin compiler options with ktlint and detekt. A subproject applies such a plugin
  by the id `de.seuhd.campuscoffee.<name>`. Only the `data` module, for example, applies `kotlin-jpa-conventions`,
  which adds the Kotlin JPA plugin.
  Code: `java-conventions.gradle.kts`, `kotlin-conventions.gradle.kts`.
- **port**: An interface in `domain/.../ports/` that connects the domain to the other modules. The controllers call the
  API ports in `ports/api/`, and the data module implements the data ports in `ports/data/`. The bootstrap admin
  loader, the tests, and the dev application call the internal port in `ports/internal/`, which creates a user without
  an acting user.
  Code: `UserService`, `UserDataService`, `UserInternalService`.
- **adapter**: A class outside the domain that connects a port to a technology. Examples are a controller, which
  handles HTTP requests and calls an API port, and a data adapter such as `UserDataServiceImpl`, which implements a
  data port with Jakarta Persistence (JPA). The accepted ADR 001 applies the term to whole modules as well and calls
  the `api` module the REST adapter and the `data` module the JPA adapter.
  Code: `UserController`, `UserDataServiceImpl`.
- **entity**: A kind of domain object that the app stores in a table of its own, such as users and consumptions. Each
  kind needs several parts, such as a domain model, a data port, a JPA entity, an entity mapper, and a migration
  (CLAUDE.md, Adding a New Entity). For the tables of later features, such as payments and expenses, some of these
  parts already exist (CLAUDE.md, Tables Without an Endpoint).
- **domain model**: A class in `domain/.../model/` that represents a concept of the business logic, such as a user, and
  has no annotations or code for the web or the database. An instance of such a class is a domain object.
  Code: `DomainModel`, `User`, `CoffeeConsumption`, `CountCorrection`.
- **DTO**: A data transfer object, that is, a class in `api/.../dtos/` that defines the JSON body of a request or a
  response. A request DTO has nullable properties, and Bean Validation rejects a missing field (`@NotNull`) before the
  controller function runs. A request DTO checks no value: The domain checks the values and throws a
  `ValidationException`. A response DTO has only non-null properties. A MapStruct mapper in `api/.../mapper/` converts
  between a domain object and the matching DTOs. `UserDtoMapper` converts a `User` to a `UserDto` and a `CreateUserDto`
  to a `User`, `ConsumptionDtoMapper` converts a `CoffeeConsumption` to a `ConsumptionDto`, and
  `CountCorrectionDtoMapper` a `CountCorrection` to a `CountCorrectionDto`. `ConsumptionDeltaDto` and
  `ConsumptionCorrectionDto` have no mapper, because the controller passes their values to the domain. `UndoStatusDto`
  has no mapper either, because the controller creates it from the result of `CoffeeConsumptionService.undoableUntil`.
  Code: `UserDto`, `CreateUserDto`, `ConsumptionDto`, `ConsumptionDeltaDto`, `UserDtoMapper`.
- **JPA entity**: A class in `data/.../persistence/entities/` that JPA maps to one table, such as `UserEntity` for the
  table `users`. Each such class has a name that ends in `Entity`, and each one except `UserBalanceEntity` and
  `KittyBalanceEntity` extends the base class `Entity`. The tables of these two classes have neither a random id nor
  timestamps. An entity mapper converts between an instance of a JPA entity and the matching domain object.
  Code: `UserEntity`, `CoffeeConsumptionEntity`, `UserEntityMapper`.
- **upsert**: The operation of a data port that inserts or updates a domain object. The operation inserts a domain
  object without an id as a new row under a new random `UUID`. For a domain object with an id, the operation updates the
  stored row with that id, or throws a `NotFoundException` when no row has that id. The operation throws a
  `DuplicationException` when a row violates a unique constraint that `ConstraintMapping.REGISTRY` lists, and a
  `DataConstraintException` when a row violates any other constraint.
  Code: `CrudDataService.upsert`, `CrudDataServiceImpl.upsert`.
- **migration**: A versioned SQL file in `data/src/main/resources/db/migration/`. At startup, Flyway applies every such
  file that it has not applied to the database before, in version order. A migration that others have applied, such as
  a pushed migration, must not change (CLAUDE.md, Database).
  Code: `V1__create_users_table.sql`, `V2__create_coffee_consumptions_table.sql`.
- **database cleaner**: The class in the source set `src/testFixtures` of the data module that deletes the rows of every
  table of the app. It reads the names of the tables from the PostgreSQL catalog and truncates them in one statement.
  The data integration, system, and acceptance tests run it before each test, and the dev application runs it on every
  start. The production jar does not contain it, so neither a client nor a configuration of a deployed app can delete
  the data.
  Code: `DatabaseCleaner.clean`.
  Avoid: data reset.
- **dev application**: The app in the dev profile with the fixture users, which `gradle :application:bootTestRun`
  starts. On every start, it runs the database cleaner and creates the fixture users through
  `UserInternalService.create` before it serves requests. Its `main` function is in the test sources of the application
  module, because it uses the test fixtures.
  Code: `DevApplication.kt`, `DevDataConfiguration`, `DevDataSeeder`.
  Avoid: fixture loader, startup loader.
- **dev profile**: The Spring profile `dev` for running the app locally. In this profile, the app serves the API and
  Swagger UI on port 8081 and connects to PostgreSQL on port 5433. The dev application runs in this profile. The tests
  run without it.
  Code: `application.yaml`, `--spring.profiles.active=dev`.
- **error response**: The response to a failed request. Its JSON body has the fields `errorCode`, `message`,
  `statusCode`, `statusMessage`, `timestamp` (in UTC), and `path`. For a domain exception, `errorCode` is the class name
  of the exception. For any other error, `errorCode` is the reason phrase of the status without spaces, such as
  `BadRequest`.
  Code: `ErrorResponse`, `GlobalExceptionHandler`, `writeErrorResponse`.
- **domain exception**: A subclass of `DomainException` in `domain/.../exceptions/`. `GlobalExceptionHandler` maps each
  of these classes to a status and uses the class name as the error code. `NotFoundException` (404) means that a lookup
  found no stored object, for example no user with the id in the path. `DuplicationException` (409) means that a request
  repeats a value that must be unique, such as the login name or the email address of another user. `ConflictException`
  (409) means that a well-formed request does not apply to the current state, such as an undo after the grace period.
  `ForbiddenException` (403) means that the acting user may not perform the action, such as creating a user or
  correcting a coffee count without the role `ADMIN`. `ValidationException` (400) means that a value in a request breaks
  a rule of the domain, such as an integer delta other than +1 or a login name of 256 characters. A request body that
  the api module cannot read gets a 400 with the error code `BadRequest` instead. Such a body lacks a required field,
  has an unknown field, or has a value of the wrong JSON type, such as a delta of 1.9. `DataConstraintException` is not
  a domain exception. A data port throws it when the database refuses a row that the domain should have refused, and the
  client gets a 500.
- **firewall**: The `StrictHttpFirewall` of Spring Security, which rejects a request with `;`, `//`, `..`, or an encoded
  slash in the path or with a control character in a header value. The firewall rejects such a request because security
  rules and Spring MVC can read it differently. A rejected request gets 400 Bad Request with the error code
  `BadRequest`, and the message of the error response does not quote the request.
  Code: `SecurityConfig.requestRejectedHandler`, `RequestRejectedExceptionHandler`.
