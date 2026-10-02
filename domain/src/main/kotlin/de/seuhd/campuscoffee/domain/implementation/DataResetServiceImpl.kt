package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Domain implementation of [DataResetService]. It clears each entity type through its data port, the coffee
 * consumptions before the users that they reference. A new entity type is cleared here, before every type that it
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
