package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Domain implementation of [CoffeeConsumptionService]. Adding a coffee ([applyDelta]) loads and locks the user's
 * consumption and stores a copy with the count raised by one, in one transaction. The lock makes a second request
 * that adds a coffee for the same user wait until this transaction ends, so that both coffees count. Without the
 * lock, both requests could read the same count.
 */
@Service
class CoffeeConsumptionServiceImpl(
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService
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
        return coffeeConsumptionDataService.upsert(current.copy(count = current.count + delta))
    }
}
