package de.seuhd.campuscoffee.data.integration

import de.seuhd.campuscoffee.data.persistence.entities.ExpenseEntity
import de.seuhd.campuscoffee.data.persistence.entities.KittyBalanceEntity
import de.seuhd.campuscoffee.data.persistence.entities.UserBalanceEntity
import de.seuhd.campuscoffee.data.persistence.repositories.ExpenseRepository
import de.seuhd.campuscoffee.data.persistence.repositories.KittyBalanceRepository
import de.seuhd.campuscoffee.data.persistence.repositories.UserBalanceRepository
import de.seuhd.campuscoffee.data.persistence.repositories.UserRepository
import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeePriceDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeRatingDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.PaymentDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * Integration test of the [DatabaseCleaner] against PostgreSQL. The test stores a row in every table of the app and
 * then reads the names of the tables from the PostgreSQL catalog. A table that a new migration creates is therefore
 * part of the assertions without a change to this class, although the test does not store a row in it.
 */
class DatabaseCleanerIntegrationTest : AbstractDataIntegrationTest() {
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

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var expenseRepository: ExpenseRepository

    @Autowired
    private lateinit var userBalanceRepository: UserBalanceRepository

    @Autowired
    private lateinit var kittyBalanceRepository: KittyBalanceRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /** Returns the number of rows of each table in the current schema, by the name of the table. */
    private fun rowCounts(): Map<String, Long?> =
        jdbcTemplate
            .queryForList("SELECT tablename FROM pg_tables WHERE schemaname = current_schema()", String::class.java)
            .filterNotNull()
            .associateWith { jdbcTemplate.queryForObject("SELECT count(*) FROM \"$it\"", Long::class.java) }

    /** Stores at least one row in every table of the app, including a merged bean and an expense of the admin. */
    private fun storeARowInEveryTable() {
        val admin = userDataService.upsert(TestFixtures.admin())
        coffeeConsumptionDataService.upsert(TestFixtures.consumptionWithRecentCoffee(admin))
        countCorrectionDataService.insert(
            CountCorrection(user = admin, admin = admin, countBefore = 2, countAfter = 1, note = "Double tap")
        )
        coffeePriceDataService.upsert(CoffeePrice(amountCents = 50))
        paymentDataService.upsert(Payment(user = admin, amountCents = 1000))
        val target = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))
        val merged = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenia AA"))
        coffeeBeanDataService.upsert(merged.copy(active = false, mergedIntoId = target.id))
        coffeeRatingDataService.upsert(CoffeeRating(user = admin, bean = target, value = 5))
        expenseRepository.saveAndFlush(
            ExpenseEntity().apply {
                id = UUID.randomUUID()
                buyer = requireNotNull(userRepository.findByIdOrNull(admin.persistedId))
                expenseType = ExpenseEntity.OTHER_TYPE
                amountCents = 350
                privateAmountCents = 350
                kittyAmountCents = 0
            }
        )
        userBalanceRepository.saveAndFlush(UserBalanceEntity().apply { userId = admin.persistedId })
        kittyBalanceRepository.saveAndFlush(KittyBalanceEntity())
    }

    @Test
    fun `clean deletes the rows of every table except the migration history of Flyway`() {
        storeARowInEveryTable()
        val before = rowCounts()
        assertThat(before.filterValues { it == 0L }).`as`("tables without a row before the cleaning").isEmpty()

        DatabaseCleaner(dataSource).clean()

        val after = rowCounts()
        assertThat(after.filterKeys { it != FLYWAY_TABLE }.values).containsOnly(0L)
        assertThat(after[FLYWAY_TABLE]).isEqualTo(before[FLYWAY_TABLE])
    }

    private companion object {
        /** The table in which Flyway records the applied migrations, which the cleaner must keep. */
        const val FLYWAY_TABLE = "flyway_schema_history"
    }
}
