package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Domain implementation of [CoffeeConsumptionService]. [applyDelta] adds a coffee in one transaction. It loads and
 * locks the consumption of the user and then stores a copy with the coffee count raised by one. A concurrent request
 * for the same user waits until the transaction ends and then reads the raised count. Without the lock, two
 * concurrent requests could both read a count of 2 and both store a count of 3.
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
