# Glossary

Coffee Kitty names each concept with one term and uses that term alike in its application programming interface (API),
its code, and its documentation. Identifiers such as class, field, table, and route names keep their spelling. Each
entry adds the API and code names where they differ from the term and, for some terms, the words to avoid.

## Domain

- **user**: A person whose coffees the app counts, with a login name, an email address, and a first and a last name.
  API: `/api/users`, `UserDto`, `userId`. Code: `User`, `UserService`, `users`.
  Avoid: member, drinker.
- **login name**: A user's unique name of letters, digits, and underscores, at most 255 characters long. The list of
  users is ordered by login name, and a coffee count is shown with the login name of its user.
  API: `loginName`. Code: `User.loginName`, `login_name`.
- **coffee**: One cup that a user drinks. Adding a coffee raises the user's coffee count by one.
  API: `POST /api/users/{userId}/consumption` with `delta` 1. Code: `CoffeeConsumptionService.applyDelta`.
  Avoid: bump (a count).
- **coffee count**: A user's running number of coffees, which starts at zero and only grows.
  API: `ConsumptionDto.count`. Code: `CoffeeConsumption.count`, `coffee_consumptions.count`.
- **consumption**: The record that holds one user's coffee count. Every user has exactly one consumption, created
  together with the user.
  API: `/api/users/{userId}/consumption`, `ConsumptionDto`. Code: `CoffeeConsumption`, `coffee_consumptions`.
- **fixture**: One of the five users that the dev profile seeds on every start and the system and acceptance tests
  seed before each test, each with a coffee count of zero.
  Code: `TestFixtures`, `FixtureStartupLoader`, `campus-coffee.fixtures.*`.

## Architecture

- **port**: An interface in `domain/.../ports/` that connects the domain to the rest of the app. The controllers call
  the API ports in `ports/api/`, and the data module implements the data ports in `ports/data/`. The fixture loader
  and the tests also use the internal ports in `ports/internal/`.
  Code: `UserService`, `UserDataService`, `DataResetService`.
- **adapter**: A class outside the domain that connects a port to a technology, such as a controller that calls an API
  port or a class that implements a data port with Jakarta Persistence (JPA).
  Code: `UserController`, `UserDataServiceImpl`.
- **module**: One of the Gradle subprojects `domain`, `api`, `data`, and `application`, each a layer in the
  architecture tests. The `detekt-rules` subproject holds build tooling and is not a layer.
- **migration**: A versioned SQL file in `data/src/main/resources/db/migration/` that Flyway applies once, in version
  order, at startup. An applied migration is never edited.
  Code: `V1__create_users_table.sql`, `V2__create_coffee_consumptions_table.sql`.
