package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

/**
 * Unit test for [DataResetServiceImpl] with Mockito mocks of [CoffeeConsumptionDataService] and [UserDataService].
 * The test checks the order of the two calls, because every consumption references a user.
 */
class DataResetServiceTest {
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService = mock()
    private val userDataService: UserDataService = mock()
    private val service = DataResetServiceImpl(coffeeConsumptionDataService, userDataService)

    @Test
    fun `clearAll clears the consumptions before the users`() {
        service.clearAll()

        inOrder(coffeeConsumptionDataService, userDataService) {
            verify(coffeeConsumptionDataService).clear()
            verify(userDataService).clear()
        }
    }
}
