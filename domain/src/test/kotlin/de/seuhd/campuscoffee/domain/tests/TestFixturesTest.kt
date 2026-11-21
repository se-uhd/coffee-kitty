package de.seuhd.campuscoffee.domain.tests

import de.seuhd.campuscoffee.domain.implementation.ConsumptionProperties
import de.seuhd.campuscoffee.domain.implementation.GracePeriod
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for [TestFixtures]. The glossary states that there are five fixture users and that only `jane_doe` is an
 * admin. The login names and the email addresses must be unique, because the database refuses a second user with the
 * same login name or email address. The last three tests check the fixed time and that, at the fixed time and with
 * the default grace period, the recent coffee is within its grace period and the old coffee is not.
 */
class TestFixturesTest {
    private val fixtureUsers = TestFixtures.getUserFixtures()

    private val gracePeriod = GracePeriod(ConsumptionProperties(), TestFixtures.fixedClock())

    @Test
    fun `getUserFixtures returns five users`() {
        assertThat(fixtureUsers).hasSize(5)
    }

    @Test
    fun `the fixture users have unique login names and unique email addresses`() {
        assertThat(fixtureUsers.map { it.loginName }).doesNotHaveDuplicates()
        assertThat(fixtureUsers.map { it.emailAddress }).doesNotHaveDuplicates()
    }

    @Test
    fun `only the fixture user jane_doe is an admin`() {
        val admins = fixtureUsers.filter { it.role == Role.ADMIN }

        assertThat(admins.map { it.loginName }).containsExactly("jane_doe")
    }

    @Test
    fun `createUserFixtures creates every fixture user through the internal port and returns the stored users`() {
        val userInternalService: UserInternalService = mock()
        val storedId = UUID(0L, 1L)
        whenever(userInternalService.create(any())).thenAnswer { (it.arguments[0] as User).copy(id = storedId) }

        val storedUsers = TestFixtures.createUserFixtures(userInternalService)

        assertThat(storedUsers).containsExactlyElementsOf(fixtureUsers.map { it.copy(id = storedId) })
    }

    @Test
    fun `fixedClock shows the fixed time in UTC`() {
        assertThat(LocalDateTime.now(TestFixtures.fixedClock())).isEqualTo(TestFixtures.FIXED_TIME)
    }

    @Test
    fun `the coffee of consumptionWithRecentCoffee is within its grace period at the fixed time`() {
        val consumption = TestFixtures.consumptionWithRecentCoffee(TestFixtures.regularUser())

        assertThat(gracePeriod.isOpen(consumption.coffeeTimes.single())).isTrue()
    }

    @Test
    fun `the grace period of the coffee of consumptionWithOldCoffee has passed at the fixed time`() {
        val consumption = TestFixtures.consumptionWithOldCoffee(TestFixtures.regularUser())

        assertThat(gracePeriod.isOpen(consumption.coffeeTimes.single())).isFalse()
    }
}
