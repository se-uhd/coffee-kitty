package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Domain implementation of [CoffeeConsumptionService]. Adding a coffee ([applyDelta]) loads the user's consumption
 * and upserts a copy with the count raised by one, in one transaction.
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
            throw ValidationException("A single-step change must be +1.")
        }
        val current = coffeeConsumptionDataService.getByUserId(userId)
        return coffeeConsumptionDataService.upsert(current.copy(count = current.count + delta))
    }
}
