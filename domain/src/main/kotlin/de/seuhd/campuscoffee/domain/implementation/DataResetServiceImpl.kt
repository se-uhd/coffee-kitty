package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeePriceDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeRatingDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.PaymentDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Domain implementation of [DataResetService]. It deletes all ratings, payments, corrections, consumptions, and users,
 * then all beans, and then the price. It deletes the rows of an entity only after the rows of every entity that
 * references it: A rating references a user and a bean, and a payment, a correction, and a consumption reference users.
 * Delete the rows of a new entity before the rows of the entities that the new entity references.
 *
 * The tables `expenses`, `user_balance`, and `kitty_balance` have no data port yet, so the data reset does not clear
 * them. A row in `expenses` references a user and can reference a bean. While `expenses` has a row, [clearAll]
 * therefore cannot delete the users. The ratings, payments, corrections, and consumptions stay deleted, because
 * [clearAll] runs without a transaction of its own. The database deletes the row in `user_balance` together with its
 * user, and the row in `kitty_balance` stays. When one of these tables gets a data port, call the `clear` of that port
 * here before the `clear` of the users.
 */
@Service
class DataResetServiceImpl(
    private val coffeeRatingDataService: CoffeeRatingDataService,
    private val paymentDataService: PaymentDataService,
    private val countCorrectionDataService: CountCorrectionDataService,
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val userDataService: UserDataService,
    private val coffeeBeanDataService: CoffeeBeanDataService,
    private val coffeePriceDataService: CoffeePriceDataService
) : DataResetService {
    override fun clearAll() {
        log.warn { "Running a data reset." }
        coffeeRatingDataService.clear()
        paymentDataService.clear()
        countCorrectionDataService.clear()
        coffeeConsumptionDataService.clear()
        userDataService.clear()
        coffeeBeanDataService.clear()
        coffeePriceDataService.clear()
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
