package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Domain implementation of [DataResetService]. It deletes all consumptions and then all users, because every
 * consumption references a user. Delete the rows of a new entity before the rows of the entities that the new entity
 * references.
 */
@Service
class DataResetServiceImpl(
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val userDataService: UserDataService
) : DataResetService {
    override fun clearAll() {
        log.warn { "Clearing all data." }
        coffeeConsumptionDataService.clear()
        userDataService.clear()
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
