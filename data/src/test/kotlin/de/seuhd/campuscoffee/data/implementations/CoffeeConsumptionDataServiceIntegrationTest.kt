package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
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
 * of [TestFixtures]. The tests check how the database stores the times of the coffees, what an undo stores, and which
 * rows the database refuses or deletes by itself.
 *
 * Every consumption needs a stored user. After the base class has cleared the database, [storeUser] therefore stores
 * a fixture user before each test.
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
    fun `upsert of a consumption without a coffee stores a count of zero and no coffee time`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithoutCoffee(user))

        val stored = coffeeConsumptionDataService.getByUserId(user.persistedId)
        assertThat(stored.count).isZero()
        assertThat(stored.coffeeTimes).isEmpty()
    }

    @Test
    fun `upsert of a consumption with a recent coffee stores the time of the coffee`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))

        val stored = coffeeConsumptionDataService.getByUserId(user.persistedId)
        assertThat(stored.coffeeTimes).containsExactly(TestFixtures.FIXED_TIME)
    }

    @Test
    fun `upsert of a consumption stores a coffee time with its microseconds`() {
        val coffeeAddedAt = TestFixtures.FIXED_TIME.plusNanos(123_456_000)
        val consumption = TestFixtures.consumptionWithRecentCoffee(user).copy(coffeeTimes = listOf(coffeeAddedAt))

        coffeeConsumptionDataService.upsert(consumption)

        assertThat(coffeeConsumptionDataService.getByUserId(user.persistedId).coffeeTimes)
            .containsExactly(coffeeAddedAt)
    }

    @Test
    fun `upsert of a consumption without its coffee time deletes the row of the time, as an undo requires`() {
        val stored = coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))

        coffeeConsumptionDataService.upsert(stored.copy(count = 0, coffeeTimes = emptyList()))

        assertThat(coffeeConsumptionDataService.getByUserId(user.persistedId).count).isZero()
        assertThat(countCoffeeTimeRows()).isZero()
    }

    @Test
    fun `upsert of a consumption with a negative count throws DataIntegrityViolationException`() {
        val consumption = TestFixtures.consumptionWithoutCoffee(user).copy(count = -1)

        assertThatThrownBy { coffeeConsumptionDataService.upsert(consumption) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("coffee_consumptions_count_check")
    }

    @Test
    fun `delete of a user deletes the coffee times of the user`() {
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(user))
        assertThat(countCoffeeTimeRows()).isEqualTo(1)

        userDataService.delete(user.persistedId)

        assertThat(countCoffeeTimeRows()).isZero()
    }
}
