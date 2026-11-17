package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import java.util.UUID

/**
 * API port for the consumptions. The controllers call it to read a user's coffee count, to add a coffee, and to undo
 * the most recent coffee. `CoffeeConsumptionServiceImpl` in the domain module implements it. [UserService.create]
 * creates the consumption of a new user with a coffee count of zero.
 */
interface CoffeeConsumptionService {
    /**
     * Returns the consumption of the user with [userId]. The consumption holds the current coffee count of the user.
     *
     * @param userId the id of the user
     * @throws NotFoundException if no user has [userId]
     */
    fun getByUserId(userId: UUID): CoffeeConsumption

    /**
     * Adds [delta] to the coffee count of the user with [userId] and returns the updated consumption. The domain
     * accepts only a delta of `+1`, which adds one coffee. With the delta parameter, an extension can accept a delta
     * of `-1` to remove a coffee without changing the request body. In the coffee app of SE@UHD, an admin removes a
     * coffee with a delta of `-1`, and a delta of `-1` at a coffee count of zero gets the status 409 Conflict.
     *
     * @param userId the id of the user
     * @param delta  the change to the coffee count, which must be `+1`
     * @throws ValidationException if [delta] is not `+1`
     * @throws NotFoundException if no user has [userId]
     */
    fun applyDelta(
        userId: UUID,
        delta: Int
    ): CoffeeConsumption

    /**
     * Undoes the most recent coffee of the user with [userId] that is not undone yet and returns the consumption with
     * the lowered coffee count. The undo succeeds until 5 minutes after that coffee, including that instant. After an
     * undo, the coffee before is the most recent coffee.
     *
     * @param userId the id of the user
     * @throws ConflictException if the user has no coffee to undo or the grace period of the most recent coffee has
     *   passed
     * @throws NotFoundException if no user has [userId]
     */
    fun undo(userId: UUID): CoffeeConsumption
}
