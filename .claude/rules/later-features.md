---
description: "the tables of later features and the rules for money (FR-4 to FR-6, FR-10 to FR-12, FR-15 to FR-19)."
paths:
  - "requirements/**"
  - "domain/src/main/kotlin/de/seuhd/campuscoffee/domain/model/**"
  - "domain/src/main/kotlin/de/seuhd/campuscoffee/domain/ports/data/**"
  - "data/src/main/**"
---

# Tables of later features

The migrations V6 to V12 create the tables of later features, which the coffee app of SE@UHD has. Their columns and
constraints follow that app without its event log. No endpoint and no domain service uses them yet.

- The price per cup, the payments, the beans, and the ratings have a domain model, a data port, an entity mapper, and a
  data adapter. An API port, a domain service, DTOs, and a controller are missing.
- `ModelConstraintsTest` checks the constraints of `CoffeePrice`, `Payment`, `CoffeeBean`, and `CoffeeRating`, because
  no domain service checks them yet. The limits of the amount of a payment depend on the kind of payment, so the domain
  service of the operation has to check them.
- The expenses and the balances have only a JPA entity and a repository with the queries and locks that a data adapter
  needs. `UserBalanceEntity` and `KittyBalanceEntity` do not extend `Entity`, because their tables have neither a random
  id nor timestamps, so neither `EntityMapper` nor `CrudDataServiceImpl` applies to them.
- `ExpenseEntity.expenseType` is a `String` with the values `BEANS` and `OTHER`, which MapStruct can map to the
  constants of a domain enum with these names.
- No migration stores a price, and every test starts with empty tables, so `CoffeePriceDataService.findCurrent` returns
  null. The startup task and the test setup have to store the start price, as `BootstrapAdminLoader` stores the
  bootstrap admin.

## Money

- Coffee Kitty does not store the price of each past coffee, so it cannot compute a balance from the stored data
  afterwards. Every operation that moves money has to change the balances in the same transaction, such as adding or
  undoing a coffee, a correction, a deposit, and an expense.
- A domain service locks a balance with `UserBalanceRepository.findForUpdateByUserId` or
  `KittyBalanceRepository.findForUpdateById` through its data adapter, as `applyDelta` locks a consumption.
- No migration inserts a balance row. The first operation that changes a balance has to insert its row, unless
  `UserServiceImpl.create` creates the row of a new user together with the consumption.
- An undo has to raise the balance by the amount by which the coffee lowered it, even when the price per cup changed in
  between (FR-11). The column `coffee_times.price_cents` (V12) holds this amount, and no JPA entity maps it yet. The
  implementation of FR-5 has to store the price with the time of each coffee, and the implementation of FR-11 has to
  use it.
- Coffee Kitty drops the time and the price of a coffee outside its grace period when the user adds a coffee, so a
  correction cannot use the price of a coffee that it removes. A correction changes the balance by the price per cup at
  the time of the correction for each coffee that it adds or removes (FR-18).
