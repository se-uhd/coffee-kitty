package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * The domain implementation of [CoffeeConsumptionService]. [applyDelta] adds a coffee in one transaction. It loads and
 * locks the consumption of the user and then stores a copy with the coffee count raised by one. A concurrent request
 * for the same user waits until the transaction ends and then reads the raised count. Without the lock, two concurrent
 * requests could both read a count of 2 and both store a count of 3. [applyDelta] appends the time of the coffee from
 * [clock], which a test can set to a fixed time. It drops the times of the coffees outside their grace period, because
 * the list of times would otherwise grow with every coffee. [undo] takes the time of the most recent coffee that the
 * user did not undo. It checks the grace period of this coffee with [gracePeriod] and removes the time. As in the
 * coffee app of SE@UHD, the user can then undo the coffee before if the grace period of that coffee is open.
 * [undoableUntil] applies the checks of [undo] without changing the consumption, because it must return a time exactly
 * when an undo at the current time succeeds. [correctCount] locks the consumption like [applyDelta] and creates a
 * [CountCorrection] with the new coffee count and the note. It checks the correction against the constraints on
 * [CountCorrection] with the [DomainValidator]. It then stores the corrected count and the correction in the same
 * transaction. A correction to a lower count removes the times of the newest coffees. The coffee app of SE@UHD removes
 * the newest coffees of the user from its undo stack in the same way. An undo after the correction then checks the
 * newest coffee that remains.
 */
@Service
class CoffeeConsumptionServiceImpl(
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val countCorrectionDataService: CountCorrectionDataService,
    private val gracePeriod: GracePeriod,
    private val clock: Clock,
    private val domainValidator: DomainValidator
) : CoffeeConsumptionService {
    override fun getByUserId(userId: UUID): CoffeeConsumption = coffeeConsumptionDataService.getByUserId(userId)

    @Transactional
    override fun applyDelta(
        userId: UUID,
        delta: Int
    ): CoffeeConsumption {
        if (delta != 1) {
            throw ValidationException("The delta must be +1.")
        }
        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
        // An Int and the column coffee_consumptions.count cannot hold a higher count. Only a correction can set a
        // count this high.
        if (current.count == Int.MAX_VALUE) {
            throw ConflictException("The coffee count is at its maximum of ${Int.MAX_VALUE}.")
        }
        return coffeeConsumptionDataService.upsert(
            current.copy(
                count = current.count + delta,
                // keep only the times within their grace period, because the user can no longer undo the other coffees
                coffeeTimes = current.coffeeTimes.filter { gracePeriod.isOpen(it) } + now()
            )
        )
    }

    @Transactional
    override fun undo(userId: UUID): CoffeeConsumption {
        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
        if (current.count <= 0) {
            throw ConflictException("There is no coffee to undo.")
        }
        // The user cannot undo a coffee without a time. The user added such a coffee before Coffee Kitty 0.2.0, a
        // correction added it, or applyDelta dropped its time after the grace period.
        val lastCoffeeAt = current.coffeeTimes.lastOrNull()
        if (lastCoffeeAt == null || !gracePeriod.isOpen(lastCoffeeAt)) {
            throw ConflictException("The grace period of the most recent coffee has passed.")
        }
        return coffeeConsumptionDataService.upsert(
            current.copy(count = current.count - 1, coffeeTimes = current.coffeeTimes.dropLast(1))
        )
    }

    override fun undoableUntil(userId: UUID): LocalDateTime? {
        val current = coffeeConsumptionDataService.getByUserId(userId)
        val lastCoffeeAt = current.coffeeTimes.lastOrNull()
        // These are the checks of undo. The schema allows a coffee count of zero with a stored coffee time, because no
        // constraint links the coffee count to the stored times. With the check of the count, the result is then null.
        if (current.count <= 0 || lastCoffeeAt == null || !gracePeriod.isOpen(lastCoffeeAt)) {
            return null
        }
        return gracePeriod.closesAt(lastCoffeeAt)
    }

    @Transactional
    override fun correctCount(
        userId: UUID,
        total: Int,
        note: String,
        actingUser: User
    ): CoffeeConsumption {
        requireAdmin(actingUser, "correct a coffee count")
        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
        val correction =
            CountCorrection(
                user = current.user,
                admin = actingUser,
                countBefore = current.count,
                countAfter = total,
                note = note
            )
        domainValidator.requireValid(correction)
        // keep every time for a higher total, because the added coffees have no time and the user cannot undo them
        val removedCoffees = (current.count - total).coerceAtLeast(0)
        val corrected =
            coffeeConsumptionDataService.upsert(
                current.copy(count = total, coffeeTimes = current.coffeeTimes.dropLast(removedCoffees))
            )
        countCorrectionDataService.insert(correction)
        return corrected
    }

    override fun getCorrections(
        userId: UUID,
        actingUser: User
    ): List<CountCorrection> {
        requireAdmin(actingUser, "list the corrections of a coffee count")
        // getAllByUserId returns an empty list for an unknown user. getByUserId throws a NotFoundException instead, and
        // the endpoint returns 404 like the other consumption endpoints.
        coffeeConsumptionDataService.getByUserId(userId)
        return countCorrectionDataService.getAllByUserId(userId)
    }

    /** Returns the current UTC time of [clock], truncated to microseconds like the timestamps in the database. */
    private fun now(): LocalDateTime =
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
}
