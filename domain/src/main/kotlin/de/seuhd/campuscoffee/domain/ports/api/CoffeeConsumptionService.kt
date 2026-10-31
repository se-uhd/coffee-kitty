package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import java.util.UUID

/**
 * API port for the consumptions, implemented in the domain module and called by the controllers: reading a user's
 * coffee count and adding a coffee. [UserService.create] creates each consumption at zero together with its user.
 */
interface CoffeeConsumptionService {
    /**
     * Returns the consumption of the user with [userId], which holds their current coffee count.
     *
     * @param userId the id of the user whose consumption to read
     * @return that user's consumption
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserId(userId: UUID): CoffeeConsumption

    /**
     * Adds a coffee for the user with [userId] by applying [delta], which must be `+1`, to their count, and returns
     * the updated consumption. The port takes a delta rather than always adding one coffee, so that an extension can
     * accept a delta of `-1` to remove a coffee without changing the request body. In the coffee app of SE@UHD, an
     * admin removes a coffee with a delta of `-1`, and a `-1` at a count of zero gets a 409 Conflict.
     *
     * @param userId the id of the user whose count to change
     * @param delta  the change to apply, which must be `+1`
     * @return the updated consumption
     * @throws ValidationException if [delta] is not `+1` (a 400)
     * @throws NotFoundException if no user has [userId]
     */
    fun applyDelta(
        userId: UUID,
        delta: Int
    ): CoffeeConsumption
}
