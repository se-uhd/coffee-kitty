package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import java.util.UUID

/**
 * Data port for the consumptions, implemented by `CoffeeConsumptionDataServiceImpl` in the data module. It
 * adds two lookups by user to the operations of [CrudDataService]. Its [getAll] returns the consumptions in the order
 * in which they were created.
 */
interface CoffeeConsumptionDataService : CrudDataService<CoffeeConsumption, UUID> {
    /**
     * Retrieves the consumption of the user with [userId]. Each user has exactly one.
     *
     * @param userId the id of the owning user
     * @return that user's consumption
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserId(userId: UUID): CoffeeConsumption

    /**
     * Retrieves the consumption of the user with [userId], as [getByUserId] does, and locks it until the caller's
     * transaction ends. A concurrent call for the same user waits until then, so two concurrent changes cannot both
     * start from the same count. Call it only inside a transaction.
     *
     * @param userId the id of the owning user
     * @return that user's consumption
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserIdForUpdate(userId: UUID): CoffeeConsumption
}
