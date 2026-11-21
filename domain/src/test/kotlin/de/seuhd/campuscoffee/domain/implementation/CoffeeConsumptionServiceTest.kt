package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.jvm.javaMethod

/**
 * Unit tests for [CoffeeConsumptionServiceImpl] with a Mockito mock of [CoffeeConsumptionDataService] and a fixed
 * clock. [serviceAt] creates the service with a clock fixed at a given time. The fixed time, the clock, the user, and
 * the consumptions without a coffee, with a recent coffee, and with an old coffee come from [TestFixtures].
 * [consumption] returns a consumption with any other count and times.
 */
class CoffeeConsumptionServiceTest {
    private val dataService: CoffeeConsumptionDataService = mock()

    private val coffeeAddedAt: LocalDateTime = TestFixtures.FIXED_TIME

    private fun serviceAt(now: LocalDateTime): CoffeeConsumptionServiceImpl {
        val clock = TestFixtures.fixedClock(now)
        return CoffeeConsumptionServiceImpl(dataService, GracePeriod(ConsumptionProperties(), clock), clock)
    }

    private val service = serviceAt(coffeeAddedAt)

    private val userId: UUID = UUID(0L, 1L)

    private val user = TestFixtures.regularUser().copy(id = userId)

    private fun consumption(
        count: Int,
        vararg coffeeTimes: LocalDateTime
    ) = CoffeeConsumption(id = UUID(0L, 500L), user = user, count = count, coffeeTimes = coffeeTimes.toList())

    @Test
    fun `getByUserId returns the consumption from the data port`() {
        whenever(dataService.getByUserId(userId)).thenReturn(consumption(2))

        assertThat(service.getByUserId(userId)).isEqualTo(consumption(2))
    }

    @Test
    fun `applyDelta of +1 increments the count of the locked consumption by one and stores the time`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = service.applyDelta(userId, 1)

        assertThat(result.count).isEqualTo(3)
        verify(dataService).upsert(consumption(3, coffeeAddedAt))
    }

    @Test
    fun `applyDelta of +1 for a user without a coffee stores a count of 1 and the time of the coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithoutCoffee(user))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        service.applyDelta(userId, 1)

        verify(dataService).upsert(TestFixtures.consumptionWithRecentCoffee(user))
    }

    @Test
    fun `applyDelta drops the times whose grace period has passed and keeps the times within it`() {
        val expired = coffeeAddedAt.minusMinutes(5).minusSeconds(1)
        val open = coffeeAddedAt.minusMinutes(5)
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2, expired, open))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        service.applyDelta(userId, 1)

        verify(dataService).upsert(consumption(3, open, coffeeAddedAt))
    }

    @Test
    fun `applyDelta stores the time of the coffee truncated to microseconds`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        serviceAt(coffeeAddedAt.plusNanos(123_456_789)).applyDelta(userId, 1)

        verify(dataService).upsert(consumption(3, coffeeAddedAt.plusNanos(123_456_000)))
    }

    @Test
    fun `applyDelta is transactional with REQUIRED propagation`() {
        val applyDelta = requireNotNull(CoffeeConsumptionServiceImpl::applyDelta.javaMethod)

        // applyDelta must read the count and store the raised count in one transaction, because the row lock lasts
        // only until the transaction ends
        assertThat(applyDelta.getAnnotation(Transactional::class.java)?.propagation).isEqualTo(Propagation.REQUIRED)
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 2])
    fun `applyDelta with a delta other than +1 throws ValidationException and writes nothing`(delta: Int) {
        assertThrows<ValidationException> { service.applyDelta(userId, delta) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo 4 minutes and 59 seconds after the coffee lowers the count from 3 to 2`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(3, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = serviceAt(coffeeAddedAt.plusMinutes(4).plusSeconds(59)).undo(userId)

        assertThat(result.count).isEqualTo(2)
    }

    @Test
    fun `undo exactly 5 minutes after the coffee lowers the count from 3 to 2`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(3, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = serviceAt(coffeeAddedAt.plusMinutes(5)).undo(userId)

        assertThat(result.count).isEqualTo(2)
        verify(dataService).upsert(argThat { count == 2 })
    }

    @Test
    fun `undo 5 minutes and 1 second after the coffee throws ConflictException and writes nothing`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(3, coffeeAddedAt))

        assertThrows<ConflictException> { serviceAt(coffeeAddedAt.plusMinutes(5).plusSeconds(1)).undo(userId) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo of the only coffee lowers the count from 1 to 0 and stores no time`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithRecentCoffee(user))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = service.undo(userId)

        assertThat(result.count).isZero()
        assertThat(result.coffeeTimes).isEmpty()
    }

    @Test
    fun `undo of a coffee added one hour ago throws ConflictException about the grace period and writes nothing`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithOldCoffee(user))

        val exception = assertThrows<ConflictException> { service.undo(userId) }

        assertThat(exception.message).isEqualTo("The grace period to undo this coffee has passed.")
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo asks the spy of the grace period only about the most recent coffee`() {
        val clock = TestFixtures.fixedClock()
        val gracePeriod = spy(GracePeriod(ConsumptionProperties(), clock))
        val earlierCoffeeAt = coffeeAddedAt.minusMinutes(1)
        whenever(dataService.getByUserIdForUpdate(userId))
            .thenReturn(consumption(2, earlierCoffeeAt, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        CoffeeConsumptionServiceImpl(dataService, gracePeriod, clock).undo(userId)

        // the spy runs the real isOpen and records its calls
        verify(gracePeriod).isOpen(coffeeAddedAt)
        verify(gracePeriod, never()).isOpen(earlierCoffeeAt)
    }

    @Test
    fun `undo without a coffee to undo throws ConflictException and writes nothing`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithoutCoffee(user))

        val exception = assertThrows<ConflictException> { service.undo(userId) }

        assertThat(exception.message).isEqualTo("There is no coffee to undo.")
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo of a coffee without a stored time throws ConflictException about the grace period`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))

        val exception = assertThrows<ConflictException> { service.undo(userId) }

        assertThat(exception.message).isEqualTo("The grace period to undo this coffee has passed.")
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo stores the lowered count without the time of the undone coffee`() {
        val earlierCoffeeAt = coffeeAddedAt.minusMinutes(1)
        whenever(dataService.getByUserIdForUpdate(userId))
            .thenReturn(consumption(3, earlierCoffeeAt, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        serviceAt(coffeeAddedAt.plusMinutes(1)).undo(userId)

        // the coffee before is now the most recent coffee, which a second undo checks against its own time
        verify(dataService).upsert(consumption(2, earlierCoffeeAt))
    }

    @Test
    fun `undo with a coffee inside the grace period but a count already at zero throws ConflictException`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(0, coffeeAddedAt))

        assertThrows<ConflictException> { service.undo(userId) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo is transactional with REQUIRED propagation`() {
        val undo = requireNotNull(CoffeeConsumptionServiceImpl::undo.javaMethod)

        // undo must check the times and store the lowered count in one transaction, because the row lock lasts only
        // until the transaction ends
        assertThat(undo.getAnnotation(Transactional::class.java)?.propagation).isEqualTo(Propagation.REQUIRED)
    }

    @Test
    fun `applyDelta for an unknown user throws NotFoundException and writes nothing`() {
        whenever(dataService.getByUserIdForUpdate(userId))
            .thenThrow(NotFoundException(User::class.java, userId))

        assertThrows<NotFoundException> { service.applyDelta(userId, 1) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo for an unknown user throws NotFoundException and writes nothing`() {
        whenever(dataService.getByUserIdForUpdate(userId))
            .thenThrow(NotFoundException(User::class.java, userId))

        assertThrows<NotFoundException> { service.undo(userId) }
        verify(dataService, never()).upsert(any())
    }
}
