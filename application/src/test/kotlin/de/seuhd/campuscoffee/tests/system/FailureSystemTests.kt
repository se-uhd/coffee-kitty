package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

/**
 * The system tests for a request during which a data adapter throws an exception. Mockito spies wrap three data
 * adapters, and a test makes one of their functions throw. Because of the spies, the tests run in a Spring context of
 * their own.
 */
class FailureSystemTests : AbstractSystemTest() {
    @MockitoSpyBean
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    @MockitoSpyBean
    private lateinit var userDataService: UserDataService

    @MockitoSpyBean
    private lateinit var countCorrectionDataService: CountCorrectionDataService

    @Test
    fun `creating a user does not store the user when storing the consumption fails`() {
        doThrow(IllegalStateException("The insert failed.")).whenever(coffeeConsumptionDataService).upsert(any())
        val newUser =
            mapOf(
                "loginName" to "new_user",
                "emailAddress" to "new.user@se.uni-heidelberg.de",
                "firstName" to "New",
                "lastName" to "User"
            )

        val response = userRequests.create(newUser)

        assertThat(response.status).isEqualTo(500)
        assertThat(userService.getAll()).hasSize(userFixtures.size)
    }

    @Test
    fun `correcting a coffee count does not change the count when storing the correction fails`() {
        val userId = userFixture("maxmustermann").persistedId
        consumptionRequests.addCoffee(userId).requireStatus(200)
        doThrow(IllegalStateException("The insert failed.")).whenever(countCorrectionDataService).insert(any())

        val response = consumptionRequests.correctCount(userId, 5, "Forgot four coffees")

        assertThat(response.status).isEqualTo(500)
        assertThat(consumptionRequests.retrieveConsumption(userId).body.count).isEqualTo(1)
    }

    @Test
    fun `listing users returns 500 Internal Server Error when looking up the acting user fails`() {
        doThrow(IllegalArgumentException("The lookup failed.")).whenever(userDataService).getByLoginName(any())

        val response = userRequests.retrieveAll(actingUser = "jane_doe")

        assertThat(response.status).isEqualTo(500)
        assertThat(response.error.errorCode).isEqualTo("InternalServerError")
        assertThat(response.error.message).doesNotContain("The lookup failed.")
    }
}
