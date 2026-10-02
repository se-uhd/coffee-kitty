package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import java.util.UUID

/**
 * Service interface for coffee consumption operations: reading a user's coffee count and adding a coffee.
 *
 * This is a port implemented by the domain layer and consumed by the API layer. [applyDelta] loads the user's
 * consumption, raises the count, and upserts it. [UserService.create] creates each consumption at zero together
 * with its user.
 */
interface CoffeeConsumptionService {
    /**
     * Returns the consumption (the current coffee count) of the user with [userId].
     *
     * @param userId the id of the user whose consumption to read
     * @return that user's consumption
     * @throws NotFoundException if no consumption exists for [userId]
     */
    fun getByUserId(userId: UUID): CoffeeConsumption

    /**
     * Adds a coffee for the user with [userId] by applying [delta], which must be `+1`, to their count, and returns
     * the updated consumption.
     *
     * @param userId the id of the user whose count to change
     * @param delta  the change to apply, which must be `+1`
     * @return the updated consumption
     * @throws ValidationException if [delta] is not `+1` (a malformed request, 400)
     * @throws NotFoundException if no consumption exists for [userId]
     */
    fun applyDelta(
        userId: UUID,
        delta: Int
    ): CoffeeConsumption
}
