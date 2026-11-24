package de.seuhd.campuscoffee.data.integration

import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeePriceDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeRatingDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.PaymentDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Integration test of the data reset against PostgreSQL. The test stores a row in every table with a data port.
 * `DataResetService.clearAll` must delete these rows in an order that violates no foreign key.
 */
class DataResetIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var dataResetService: DataResetService

    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    @Autowired
    private lateinit var countCorrectionDataService: CountCorrectionDataService

    @Autowired
    private lateinit var coffeePriceDataService: CoffeePriceDataService

    @Autowired
    private lateinit var paymentDataService: PaymentDataService

    @Autowired
    private lateinit var coffeeBeanDataService: CoffeeBeanDataService

    @Autowired
    private lateinit var coffeeRatingDataService: CoffeeRatingDataService

    @Test
    fun `clearAll deletes the rows of every table with a data port`() {
        val admin =
            userDataService.upsert(
                User(loginName = "ada", emailAddress = "ada@se.de", firstName = "A", lastName = "L", role = Role.ADMIN)
            )
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = admin, count = 1))
        countCorrectionDataService.upsert(
            CountCorrection(user = admin, admin = admin, countBefore = 2, countAfter = 1, note = "Double tap")
        )
        coffeePriceDataService.upsert(CoffeePrice(amountCents = 50))
        paymentDataService.upsert(Payment(user = admin, amountCents = 1000))
        val target = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))
        val merged = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenia AA"))
        coffeeBeanDataService.upsert(merged.copy(active = false, mergedIntoId = target.id))
        coffeeRatingDataService.upsert(CoffeeRating(user = admin, bean = target, value = 5))

        dataResetService.clearAll()

        assertThat(coffeeRatingDataService.getAll()).isEmpty()
        assertThat(paymentDataService.getAll()).isEmpty()
        assertThat(countCorrectionDataService.getAll()).isEmpty()
        assertThat(coffeeConsumptionDataService.getAll()).isEmpty()
        assertThat(userDataService.getAll()).isEmpty()
        assertThat(coffeeBeanDataService.getAll()).isEmpty()
        assertThat(coffeePriceDataService.findCurrent()).isNull()
    }
}
