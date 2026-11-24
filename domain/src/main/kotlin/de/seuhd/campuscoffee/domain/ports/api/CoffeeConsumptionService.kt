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
 * API port for the consumptions. The controllers call it to read and correct coffee counts, to add and undo coffees,
 * and to read the undo status and the corrections. `CoffeeConsumptionServiceImpl` in the domain module implements it.
 * [UserService.create] creates the consumption of a new user with a coffee count of zero.
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
     * of `-1` to remove a coffee without changing the format of the request body. In the coffee app of SE@UHD, an admin
     * removes a coffee with a delta of `-1`. At a coffee count of zero, this delta gets the status 409 Conflict.
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
     * Undoes the most recent coffee of the user with [userId] and returns the consumption with the lowered coffee
     * count. The undo succeeds until the grace period of the coffee ends, including that instant. After an undo, the
     * coffee before is the most recent coffee.
     *
     * @param userId the id of the user
     * @throws ConflictException if the user has no coffee to undo or the grace period of the most recent coffee has
     *   passed
     * @throws NotFoundException if no user has [userId]
     */
    fun undo(userId: UUID): CoffeeConsumption

    /**
     * Returns the time until which the user with [userId] can undo their most recent coffee, or null if the user cannot
     * undo a coffee at the current time (FR-9). The result is a time exactly when [undo] at the current time succeeds.
     * The time is then the end of the grace period of the coffee that this undo removes. The function reads the
     * consumption without a lock and does not change it.
     *
     * @param userId the id of the user
     * @throws NotFoundException if no user has [userId]
     */
    fun undoableUntil(userId: UUID): LocalDateTime?

    /**
     * Corrects the coffee count of the user with [userId] to [total] and stores the correction with [note] (FR-8).
     * [actingUser] has to be an admin and can correct a coffee count at any time, even after the grace period. A
     * [total] below the coffee count removes the newest coffees of the user, and an undo then applies to the newest
     * coffee that remains. A [total] above the coffee count adds coffees that the user cannot undo. The coffee app of
     * SE@UHD has a function with the same name.
     *
     * @param userId     the id of the user whose coffee count the admin corrects
     * @param total      the corrected coffee count, from 0 to [CountCorrection.MAX_TOTAL]
     * @param note       the note with the reason for the correction, which must not be blank
     * @param actingUser the user who sends the request
     * @return the consumption with the corrected coffee count
     * @throws ForbiddenException if [actingUser] is not an admin
     * @throws ValidationException if [total] is negative or above [CountCorrection.MAX_TOTAL], if [note] is blank, or
     *   if [note] has more than [CountCorrection.MAX_NOTE_LENGTH] characters or the NUL character
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
