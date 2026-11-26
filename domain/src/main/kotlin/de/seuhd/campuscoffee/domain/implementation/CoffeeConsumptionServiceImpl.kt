package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.StaleDataException
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
 * The domain implementation of [CoffeeConsumptionService]. [applyDelta], [undo], and [correctCount] lock the
 * consumption of the user until their transaction ends. Without the lock, two concurrent requests could both read a
 * count of 2 and both store a count of 3. [applyDelta] drops the times of the coffees outside their grace period,
 * because the list of times would otherwise grow with every coffee. A correction to a lower count removes the times of
 * the newest coffees, as the coffee app of SE@UHD removes them from its undo stack.
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
        if (current.count == Int.MAX_VALUE) {
            throw ConflictException("The coffee count is at its maximum of ${Int.MAX_VALUE}.")
        }
        return coffeeConsumptionDataService.upsert(
            current.copy(
                count = current.count + delta,
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
        if (current.count <= 0 || lastCoffeeAt == null || !gracePeriod.isOpen(lastCoffeeAt)) {
            return null
        }
        return gracePeriod.closesAt(lastCoffeeAt)
    }

    @Transactional
    override fun correctCount(
        userId: UUID,
        total: Int,
        note: String?,
        actingUser: User,
        expectedCount: Int?
    ): CoffeeConsumption {
        requireAdmin(actingUser, "correct a coffee count")
        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
        val correction =
            CountCorrection(
                user = current.user,
                admin = actingUser,
                countBefore = current.count,
                countAfter = total,
                note = note?.takeUnless { it.isBlank() }
            )
        domainValidator.requireValid(correction)
        if (expectedCount != null && expectedCount != current.count) {
            throw StaleDataException("The coffee count is ${current.count}, not the expected $expectedCount.")
        }
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
        requireUserExists(userId)
        return countCorrectionDataService.getAllByUserId(userId)
    }

    private fun requireUserExists(userId: UUID) {
        coffeeConsumptionDataService.getByUserId(userId)
    }

    private fun now(): LocalDateTime =
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
}
