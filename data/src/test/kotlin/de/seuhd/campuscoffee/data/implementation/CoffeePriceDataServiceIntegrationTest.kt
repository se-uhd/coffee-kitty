package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.ports.data.CoffeePriceDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The integration tests of [CoffeePriceDataServiceImpl] against PostgreSQL. The table `coffee_prices` holds at most one
 * price.
 */
class CoffeePriceDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var coffeePriceDataService: CoffeePriceDataService

    @Test
    fun `findCurrent returns null without a stored price`() {
        assertThat(coffeePriceDataService.findCurrent()).isNull()
    }

    @Test
    fun `upsert stores a new price with an id and timestamps`() {
        val created = coffeePriceDataService.upsert(CoffeePrice(amountCents = 50))

        assertThat(created.amountCents).isEqualTo(50)
        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isEqualTo(created.createdAt)
        assertThat(coffeePriceDataService.findCurrent()).isEqualTo(created)
    }

    @Test
    fun `upsert stores a new amount of the price`() {
        val created = coffeePriceDataService.upsert(CoffeePrice(amountCents = 50))

        val updated = coffeePriceDataService.upsert(created.copy(amountCents = 0, createdAt = null, updatedAt = null))

        assertThat(updated.id).isEqualTo(created.id)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)
        assertThat(coffeePriceDataService.findCurrent()?.amountCents).isEqualTo(0)
    }

    @Test
    fun `upsert throws DuplicationException for a second price`() {
        val first = coffeePriceDataService.upsert(CoffeePrice(amountCents = 50))

        assertThatThrownBy { coffeePriceDataService.upsert(CoffeePrice(amountCents = 60)) }
            .isInstanceOf(DuplicationException::class.java)
        assertThat(coffeePriceDataService.getAll()).containsExactly(first)
    }

    @Test
    fun `upsert throws DataConstraintException for a negative price`() {
        assertThatThrownBy { coffeePriceDataService.upsert(CoffeePrice(amountCents = -1)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(coffeePriceDataService.findCurrent()).isNull()
    }
}
