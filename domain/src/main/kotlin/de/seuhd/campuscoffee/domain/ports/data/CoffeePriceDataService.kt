package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.model.CoffeePrice
import java.util.UUID

/**
 * Data port for the price per cup, with a lookup of the stored price in addition to the operations of
 * [CrudDataService]. `CoffeePriceDataServiceImpl` in the data module implements it. The table `coffee_prices` holds at
 * most one row. [getAll] therefore returns an empty list or one price, and [upsert] throws a `DuplicationException` for
 * a second price without an id.
 */
interface CoffeePriceDataService : CrudDataService<CoffeePrice, UUID> {
    /**
     * Returns the stored price, or null if no price is stored. Null does not mean that a coffee is free. No migration
     * stores a price, and the tests start with an empty table. The startup and the test setup therefore have to store
     * the start price, as `BootstrapAdminLoader` stores the first admin.
     */
    fun findCurrent(): CoffeePrice?
}
