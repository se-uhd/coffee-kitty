package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Integration tests of the data adapter [CoffeeConsumptionDataServiceImpl] against PostgreSQL, with the consumptions
 * of [TestFixtures]. Every consumption needs a stored user. Before each test, the base class cleans the database, and
 * [storeUser] then stores a fixture user.
 */
class CoffeeConsumptionDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /** The stored fixture user of the current test. */
    private lateinit var user: User

    @BeforeEach
    fun storeUser() {
        user = userDataService.upsert(TestFixtures.regularUser())
    }

    /** Returns the number of rows in the table `coffee_times`. */
    private fun countCoffeeTimeRows(): Int? =
        jdbcTemplate.queryForObject("SELECT count(*) FROM coffee_times", Int::class.java)

    @Test
    fun `upsert stores a consumption without a coffee`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithoutCoffee(user))

        val stored = coffeeConsumptionDataService.getByUserId(user.persistedId)
        assertThat(stored.count).isZero()
        assertThat(stored.coffeeTimes).isEmpty()
    }

    @Test
    fun `upsert stores the microseconds of a coffee time`() {
        val coffeeAddedAt = TestFixtures.FIXED_TIME.plusNanos(123_456_000)
        val consumption = TestFixtures.consumptionWithRecentCoffee(user).copy(coffeeTimes = listOf(coffeeAddedAt))

        coffeeConsumptionDataService.upsert(consumption)

        assertThat(coffeeConsumptionDataService.getByUserId(user.persistedId).coffeeTimes)
            .containsExactly(coffeeAddedAt)
    }

    @Test
    fun `upsert deletes the row of a removed coffee time`() {
        val stored = coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))

        coffeeConsumptionDataService.upsert(stored.copy(count = 0, coffeeTimes = emptyList()))

        assertThat(coffeeConsumptionDataService.getByUserId(user.persistedId).count).isZero()
        assertThat(countCoffeeTimeRows()).isZero()
    }

    @Test
    fun `upsert throws DataConstraintException for a negative count`() {
        val consumption = TestFixtures.consumptionWithoutCoffee(user).copy(count = -1)

        assertThatThrownBy { coffeeConsumptionDataService.upsert(consumption) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `setting a negative price for a coffee time throws DataIntegrityViolationException`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))

        // no entity maps the column price_cents yet, so the test writes it with SQL
        assertThatThrownBy { jdbcTemplate.update("UPDATE coffee_times SET price_cents = -1") }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `setting a price of 0 or 50 cents for a coffee time stores the price`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))

        for (priceCents in listOf(0, 50)) {
            jdbcTemplate.update("UPDATE coffee_times SET price_cents = ?", priceCents)

            assertThat(jdbcTemplate.queryForObject("SELECT price_cents FROM coffee_times", Int::class.java))
                .isEqualTo(priceCents)
        }
    }

    @Test
    fun `deleting a user deletes the coffee times of the user`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))
        assertThat(countCoffeeTimeRows()).isEqualTo(1)

        userDataService.delete(user.persistedId)

        assertThat(countCoffeeTimeRows()).isZero()
    }
}
