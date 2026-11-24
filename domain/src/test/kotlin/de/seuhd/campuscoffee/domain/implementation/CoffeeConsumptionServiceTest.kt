package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

/**
 * The unit tests for [CoffeeConsumptionServiceImpl] with Mockito mocks of [CoffeeConsumptionDataService] and
 * [CountCorrectionDataService] and a fixed clock. [serviceAt] creates the service with a clock fixed at a given time.
 * [TestFixtures] holds the users, the fixed time, the clock, and the consumptions without a coffee, with a recent
 * coffee, and with an old coffee. [consumption] returns a consumption with any other count and times.
 */
class CoffeeConsumptionServiceTest {
    private val dataService: CoffeeConsumptionDataService = mock()
    private val correctionDataService: CountCorrectionDataService = mock()

    private val coffeeAddedAt: LocalDateTime = TestFixtures.FIXED_TIME

    private fun serviceAt(now: LocalDateTime): CoffeeConsumptionServiceImpl {
        val clock = TestFixtures.fixedClock(now)
        return CoffeeConsumptionServiceImpl(
            dataService,
            correctionDataService,
            GracePeriod(ConsumptionProperties(), clock),
            clock
        )
    }

    private val service = serviceAt(coffeeAddedAt)

    private val userId: UUID = UUID(0L, 1L)

    private val user = TestFixtures.regularUser().copy(id = userId)

    private val admin = TestFixtures.admin().copy(id = UUID(0L, 2L))

    private val note = "Double tap on 16.11."

    private fun consumption(
        count: Int,
        vararg coffeeTimes: LocalDateTime
    ) = CoffeeConsumption(id = UUID(0L, 500L), user = user, count = count, coffeeTimes = coffeeTimes.toList())

    /** Stubs `getByUserIdForUpdate` to return [current] and `upsert` on both mocks to return its argument. */
    private fun stubCorrection(current: CoffeeConsumption) {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(current)
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }
        whenever(correctionDataService.insert(any())).thenAnswer { it.arguments[0] as CountCorrection }
    }

    /** Asserts that the service stored neither a consumption nor a correction. */
    private fun verifyNothingStored() {
        verify(dataService, never()).upsert(any())
        verify(correctionDataService, never()).insert(any())
    }

    @Test
    fun `applyDelta raises the count by one and stores the time of the coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = service.applyDelta(userId, 1)

        assertThat(result.count).isEqualTo(3)
        verify(dataService).upsert(consumption(3, coffeeAddedAt))
    }

    @Test
    fun `applyDelta stores a count of 1 and the time of the first coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithoutCoffee(user))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        service.applyDelta(userId, 1)

        verify(dataService).upsert(TestFixtures.consumptionWithRecentCoffee(user))
    }

    @Test
    fun `applyDelta removes the times of the coffees outside their grace period`() {
        val expiredCoffeeAt = coffeeAddedAt.minusMinutes(5).minusSeconds(1)
        val openCoffeeAt = coffeeAddedAt.minusMinutes(5)
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2, expiredCoffeeAt, openCoffeeAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        service.applyDelta(userId, 1)

        verify(dataService).upsert(consumption(3, openCoffeeAt, coffeeAddedAt))
    }

    @Test
    fun `applyDelta truncates the time of the coffee to microseconds`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        serviceAt(coffeeAddedAt.plusNanos(123_456_789)).applyDelta(userId, 1)

        verify(dataService).upsert(consumption(3, coffeeAddedAt.plusNanos(123_456_000)))
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 2])
    fun `applyDelta throws ValidationException for a delta other than +1`(delta: Int) {
        assertThrows<ValidationException> { service.applyDelta(userId, delta) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo lowers the count from 3 to 2 exactly 5 minutes after the coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(3, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        val result = serviceAt(coffeeAddedAt.plusMinutes(5)).undo(userId)

        assertThat(result.count).isEqualTo(2)
        verify(dataService).upsert(consumption(2))
    }

    @Test
    fun `undo throws ConflictException 5 minutes and 1 second after the coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(3, coffeeAddedAt))

        assertThrows<ConflictException> { serviceAt(coffeeAddedAt.plusMinutes(5).plusSeconds(1)).undo(userId) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo lowers the count to 0 and removes the time of the only coffee`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithRecentCoffee(user))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        service.undo(userId)

        verify(dataService).upsert(TestFixtures.consumptionWithoutCoffee(user))
    }

    @Test
    fun `undo throws ConflictException for a coffee added one hour ago`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(TestFixtures.consumptionWithOldCoffee(user))

        assertThrows<ConflictException> { service.undo(userId) }

        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo throws ConflictException for a coffee without a stored time`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(2))

        assertThrows<ConflictException> { service.undo(userId) }

        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `undo removes only the time of the most recent coffee`() {
        val earlierCoffeeAt = coffeeAddedAt.minusMinutes(5)
        whenever(dataService.getByUserIdForUpdate(userId))
            .thenReturn(consumption(3, earlierCoffeeAt, coffeeAddedAt))
        whenever(dataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }

        serviceAt(coffeeAddedAt.plusMinutes(1)).undo(userId)

        verify(dataService).upsert(consumption(2, earlierCoffeeAt))
    }

    @Test
    fun `undo throws ConflictException at a count of zero with a stored time`() {
        whenever(dataService.getByUserIdForUpdate(userId)).thenReturn(consumption(0, coffeeAddedAt))

        assertThrows<ConflictException> { service.undo(userId) }
        verify(dataService, never()).upsert(any())
    }

    @Test
    fun `correctCount stores the corrected count and the correction`() {
        stubCorrection(consumption(3))

        val result = service.correctCount(userId, 1, note, admin)

        assertThat(result.count).isEqualTo(1)
        verify(dataService).upsert(consumption(1))
        verify(correctionDataService).insert(
            CountCorrection(user = user, admin = admin, countBefore = 3, countAfter = 1, note = note)
        )
    }

    @Test
    fun `correctCount removes the times of the newest coffees for a lower count`() {
        val firstAt = coffeeAddedAt.minusMinutes(2)
        val secondAt = coffeeAddedAt.minusMinutes(1)
        stubCorrection(consumption(3, firstAt, secondAt, coffeeAddedAt))

        service.correctCount(userId, 1, note, admin)

        // an undo after the correction checks the time of the oldest coffee, which is the only one left
        verify(dataService).upsert(consumption(1, firstAt))
    }

    @Test
    fun `correctCount removes all times when it removes more coffees than there are times`() {
        // a consumption with a count of 5 can have only two times, because applyDelta drops the times of the coffees
        // outside their grace period
        stubCorrection(consumption(5, coffeeAddedAt.minusMinutes(1), coffeeAddedAt))

        service.correctCount(userId, 2, note, admin)

        verify(dataService).upsert(consumption(2))
    }

    @Test
    fun `correctCount does not change the times for a higher count`() {
        stubCorrection(consumption(1, coffeeAddedAt))

        service.correctCount(userId, 4, note, admin)

        verify(dataService).upsert(consumption(4, coffeeAddedAt))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, CountCorrection.MAX_TOTAL])
    fun `correctCount stores a count of 0 or the maximum total`(total: Int) {
        stubCorrection(consumption(3))

        service.correctCount(userId, total, note, admin)

        verify(dataService).upsert(consumption(total))
    }

    @Test
    fun `correctCount stores a correction to the current count`() {
        stubCorrection(consumption(3, coffeeAddedAt))

        val result = service.correctCount(userId, 3, note, admin)

        assertThat(result).isEqualTo(consumption(3, coffeeAddedAt))
        verify(correctionDataService).insert(
            CountCorrection(user = user, admin = admin, countBefore = 3, countAfter = 3, note = note)
        )
    }

    @Test
    fun `correctCount throws ForbiddenException for a user who is not an admin`() {
        assertThrows<ForbiddenException> { service.correctCount(userId, 1, note, user) }

        verifyNothingStored()
    }

    @Test
    fun `correctCount throws ValidationException for a negative count`() {
        assertThrows<ValidationException> { service.correctCount(userId, -1, note, admin) }

        verifyNothingStored()
    }

    @Test
    fun `correctCount throws ValidationException for a count above the maximum total`() {
        assertThrows<ValidationException> { service.correctCount(userId, CountCorrection.MAX_TOTAL + 1, note, admin) }

        verifyNothingStored()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\t\n"])
    fun `correctCount throws ValidationException for a blank note`(blankNote: String) {
        assertThrows<ValidationException> { service.correctCount(userId, 1, blankNote, admin) }

        verifyNothingStored()
    }

    @Test
    fun `correctCount throws ValidationException for a note of 501 characters`() {
        assertThrows<ValidationException> { service.correctCount(userId, 1, "a".repeat(501), admin) }
        verifyNothingStored()
    }

    @Test
    fun `correctCount throws ValidationException for a note with a NUL character`() {
        // PostgreSQL cannot store NUL in a varchar column. Without the check, the request would get a 500.
        assertThrows<ValidationException> { service.correctCount(userId, 1, "Double\u0000tap", admin) }
        verifyNothingStored()
    }

    @Test
    fun `correctCount stores a note of 500 characters with a line break`() {
        stubCorrection(consumption(3))
        val longNote = "Double tap\n" + "a".repeat(489)

        service.correctCount(userId, 1, longNote, admin)

        verify(correctionDataService).insert(argThat { note == longNote })
    }

    @Test
    fun `getCorrections returns the corrections of the user for an admin`() {
        val correction = CountCorrection(user = user, admin = admin, countBefore = 3, countAfter = 1, note = note)
        whenever(dataService.getByUserId(userId)).thenReturn(consumption(1))
        whenever(correctionDataService.getAllByUserId(userId)).thenReturn(listOf(correction))

        assertThat(service.getCorrections(userId, admin)).containsExactly(correction)
    }

    @Test
    fun `getCorrections throws ForbiddenException for a user who is not an admin`() {
        assertThrows<ForbiddenException> { service.getCorrections(userId, user) }
    }

    @Test
    fun `getCorrections throws NotFoundException for an unknown user`() {
        whenever(dataService.getByUserId(userId)).thenThrow(NotFoundException(User::class.java, userId))

        assertThrows<NotFoundException> { service.getCorrections(userId, admin) }
    }
}
