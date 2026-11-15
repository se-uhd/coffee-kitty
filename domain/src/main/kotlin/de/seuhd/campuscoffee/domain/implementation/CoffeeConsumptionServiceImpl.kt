package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Domain implementation of [CoffeeConsumptionService]. [applyDelta] adds a coffee in one transaction. It loads and
 * locks the consumption of the user and then stores a copy with the coffee count raised by one. A concurrent request
 * for the same user waits until the transaction ends and then reads the raised count. Without the lock, two
 * concurrent requests could both read a count of 2 and both store a count of 3.
 *
 * [applyDelta] appends the time of the coffee from [clock], which a test can set to a fixed time, and drops the times
 * whose grace period has passed, so that the list of times does not grow with every coffee. [undo] takes the time of
 * the most recent coffee that is not undone, checks its grace period with [gracePeriod], and removes the time.
 * As in the coffee app of SE@UHD, the coffee before can then be undone while its own grace period is open.
 */
@Service
class CoffeeConsumptionServiceImpl(
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val gracePeriod: GracePeriod,
    private val clock: Clock
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
        return coffeeConsumptionDataService.upsert(
            current.copy(
                count = current.count + delta,
                // keep only the times within their grace period, because the other coffees can no longer be undone
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
        // a coffee without a time cannot be undone: it was added before Coffee Kitty 0.2.0, or applyDelta dropped its
        // time after the grace period
        val lastCoffeeAt = current.coffeeTimes.lastOrNull()
        if (lastCoffeeAt == null || !gracePeriod.isOpen(lastCoffeeAt)) {
            throw ConflictException("The grace period to undo this coffee has passed.")
        }
        return coffeeConsumptionDataService.upsert(
            current.copy(count = current.count - 1, coffeeTimes = current.coffeeTimes.dropLast(1))
        )
    }

    /** Returns the current UTC time of [clock], truncated to microseconds like the timestamps in the database. */
    private fun now(): LocalDateTime =
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
}
