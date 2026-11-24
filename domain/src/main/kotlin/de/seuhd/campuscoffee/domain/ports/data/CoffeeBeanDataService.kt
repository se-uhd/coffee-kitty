package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.model.CoffeeBean
import java.util.UUID

/**
 * The data port for the beans, with a lookup of a live bean by name in addition to the operations of [CrudDataService].
 * `CoffeeBeanDataServiceImpl` in the data module implements it. [getAll] returns the live and the merged beans, ordered
 * by creation time. [upsert] throws a `DuplicationException` for a live bean with the same name as another live bean,
 * ignoring case.
 */
interface CoffeeBeanDataService : CrudDataService<CoffeeBean, UUID> {
    /**
     * Returns the live bean with [name], ignoring case, or null if no live bean has that name. A merged bean never
     * matches. The method does not trim [name].
     *
     * @param name the name to look up
     */
    fun findActiveByName(name: String): CoffeeBean?
}
