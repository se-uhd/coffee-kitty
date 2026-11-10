package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import java.util.UUID

/**
 * Data port for the consumptions, with two lookups by user in addition to the operations of [CrudDataService].
 * `CoffeeConsumptionDataServiceImpl` in the data module implements it. [getAll] returns the consumptions in the order
 * in which they were created.
 */
interface CoffeeConsumptionDataService : CrudDataService<CoffeeConsumption, UUID> {
    /**
     * Returns the consumption of the user with [userId].
     *
     * @param userId the id of the user
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserId(userId: UUID): CoffeeConsumption

    /**
     * Returns the consumption of the user with [userId] and locks the consumption row until the current transaction
     * ends. A concurrent call for the same user waits for the lock and then reads the current count. Call this method
     * only inside a transaction. Outside a transaction, the data adapter throws an `IllegalTransactionStateException`.
     *
     * @param userId the id of the user
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserIdForUpdate(userId: UUID): CoffeeConsumption
}
