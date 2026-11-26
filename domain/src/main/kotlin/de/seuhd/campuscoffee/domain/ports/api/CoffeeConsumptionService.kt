package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.User
import java.time.LocalDateTime
import java.util.UUID

/**
 * The API port for the consumptions. The controllers call it to read and correct coffee counts, to add and undo
 * coffees, and to read the undo status and the corrections. `CoffeeConsumptionServiceImpl` in the domain module
 * implements it. [UserService.create] creates the consumption of a new user with a coffee count of zero.
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
     * accepts only `+1`. An extension can accept `-1` to remove a coffee without a change to the request body, as an
     * admin does in the coffee app of SE@UHD, where `-1` at a coffee count of zero gets 409 Conflict.
     *
     * @param userId the id of the user
     * @param delta  the change to the coffee count, which must be `+1`
     * @throws ValidationException if [delta] is not `+1`
     * @throws NotFoundException if no user has [userId]
     * @throws ConflictException if the coffee count is 2,147,483,647, the largest count that Coffee Kitty can store
     */
    fun applyDelta(
        userId: UUID,
        delta: Int
    ): CoffeeConsumption

    /**
     * Undoes the most recent coffee of the user with [userId] and returns the consumption with the lowered coffee
     * count. The undo succeeds until the grace period of the coffee ends, including that instant. After an undo, the
     * coffee before is the most recent coffee.
     *
     * @param userId the id of the user
     * @throws ConflictException if the user has no coffee or the grace period of the most recent coffee has passed
     * @throws NotFoundException if no user has [userId]
     */
    fun undo(userId: UUID): CoffeeConsumption

    /**
     * Returns the time until which the user with [userId] can undo their most recent coffee, or null if [undo] at the
     * current time fails (FR-9). The time is the end of the grace period of the coffee that [undo] would remove. The
     * function reads the consumption without a lock and does not change it.
     *
     * @param userId the id of the user
     * @throws NotFoundException if no user has [userId]
     */
    fun undoableUntil(userId: UUID): LocalDateTime?

    /**
     * Corrects the coffee count of the user with [userId] to [total] and stores the correction with [note] (FR-8),
     * also after the grace period. A lower [total] removes the newest coffees, and an undo then applies to the newest
     * coffee that remains. A higher [total] adds coffees that the user cannot undo.
     *
     * @param userId     the id of the user whose coffee count the admin corrects
     * @param total      the corrected coffee count, which must not be negative
     * @param note       the note with the reason for the correction, which must not be blank
     * @param actingUser the user who sends the request
     * @return the consumption with the corrected coffee count
     * @throws ForbiddenException if [actingUser] is not an admin
     * @throws ValidationException if [total] is negative or [note] breaks a constraint on [CountCorrection]
     * @throws NotFoundException if no user has [userId]
     */
    fun correctCount(
        userId: UUID,
        total: Int,
        note: String,
        actingUser: User
    ): CoffeeConsumption

    /**
     * Returns the corrections of the coffee count of the user with [userId], newest first. [actingUser] has to be an
     * admin.
     *
     * @param userId     the id of the user whose corrections to return
     * @param actingUser the user who sends the request
     * @throws ForbiddenException if [actingUser] is not an admin
     * @throws NotFoundException if no user has [userId]
     */
    fun getCorrections(
        userId: UUID,
        actingUser: User
    ): List<CountCorrection>
}
