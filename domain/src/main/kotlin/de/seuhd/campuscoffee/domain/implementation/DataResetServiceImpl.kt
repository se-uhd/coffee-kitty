package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Domain implementation of [DataResetService]. It deletes all corrections, consumptions, and users. A correction and a
 * consumption reference users and are therefore deleted before the users. Delete the rows of a new entity before the
 * rows of the entities that the new entity references.
 */
@Service
class DataResetServiceImpl(
    private val countCorrectionDataService: CountCorrectionDataService,
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val userDataService: UserDataService
) : DataResetService {
    override fun clearAll() {
        log.warn { "Running a data reset." }
        countCorrectionDataService.clear()
        coffeeConsumptionDataService.clear()
        userDataService.clear()
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
