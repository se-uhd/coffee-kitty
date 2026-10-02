package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.reflect.jvm.javaMethod

/**
 * Unit tests for [CoffeeConsumptionServiceImpl], mocking the coffee consumption data port, which echoes what it
 * stores.
 */
class CoffeeConsumptionServiceTest {
    private val dataService: CoffeeConsumptionDataService = mock()
    private val service = CoffeeConsumptionServiceImpl(dataService)

    private val userId: UUID = UUID(0L, 1L)

    private val user =
        User(
            id = userId,
            loginName = "max",
            emailAddress = "max@se.de",
            firstName = "Max",
            lastName = "M"
        )

    private fun consumption(count: Int) = CoffeeConsumption(id = UUID(0L, 500L), user = user, count = count)

    @Test
    fun `getByUserId returns the consumption from the data service`() {
        whenever(dataService.getByUserId(userId)).thenReturn(consumption(2))

        assertThat(service.getByUserId(userId)).isEqualTo(consumption(2))
    }

    @Test
    fun `applyDelta of +1 increments the count by one`() {
        whenever(dataService.getByUserId(userId)).thenReturn(consumption(2))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = service.applyDelta(userId, 1)

        assertThat(result.count).isEqualTo(3)
        verify(dataService).upsert(consumption(3))
    }

    @Test
    fun `applyDelta is transactional with REQUIRED propagation`() {
        val applyDelta = requireNotNull(CoffeeConsumptionServiceImpl::applyDelta.javaMethod)

        // loading the count and storing the raised count share one transaction
        assertThat(applyDelta.getAnnotation(Transactional::class.java)?.propagation).isEqualTo(Propagation.REQUIRED)
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 2])
    fun `applyDelta with a delta other than +1 throws ValidationException and writes nothing`(delta: Int) {
        assertThrows<ValidationException> { service.applyDelta(userId, delta) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `applyDelta for an unknown user throws NotFoundException and writes nothing`() {
        whenever(dataService.getByUserId(userId))
            .thenThrow(NotFoundException(CoffeeConsumption::class.java, "user_id", userId.toString()))

        assertThrows<NotFoundException> { service.applyDelta(userId, 1) }
        verify(dataService, never()).upsert(any())
    }
}
