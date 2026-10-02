package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.FixturesProperties
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for the fixture startup loader: It creates the fixture users only when the database has no users yet,
 * and with `reset-on-startup` it first clears the data and then creates them whatever the database holds.
 */
class FixtureStartupLoaderTest {
    private val userService = mock<UserService>()
    private val dataResetService = mock<DataResetService>()
    private val fixtureLogins = TestFixtures.getUserFixtures().map { it.loginName }

    /** A loader with `load-on-startup` set and the given `reset-on-startup` flag. */
    private fun loader(resetOnStartup: Boolean = false) =
        FixtureStartupLoader(
            userService,
            dataResetService,
            FixturesProperties(loadOnStartup = true, resetOnStartup = resetOnStartup)
        )

    @Test
    fun `loadOnStartup creates the fixture users when the database is empty`() {
        whenever(userService.getAll()).thenReturn(emptyList())
        whenever(userService.create(any())).thenAnswer { it.arguments[0] as User }

        loader().loadOnStartup()

        val created = argumentCaptor<User>()
        verify(userService, times(fixtureLogins.size)).create(created.capture())
        assertThat(created.allValues.map { it.loginName }).containsExactlyElementsOf(fixtureLogins)
        verify(dataResetService, never()).clearAll()
    }

    @Test
    fun `loadOnStartup creates no user when the database already has users`() {
        whenever(userService.getAll()).thenReturn(listOf(TestFixtures.getUserFixtures().first()))

        loader().loadOnStartup()

        verify(userService, never()).create(any())
        verify(dataResetService, never()).clearAll()
    }

    @Test
    fun `loadOnStartup clears the data and then creates the fixture users when reset-on-startup is set`() {
        whenever(userService.getAll()).thenReturn(listOf(TestFixtures.getUserFixtures().first()))
        whenever(userService.create(any())).thenAnswer { it.arguments[0] as User }

        loader(resetOnStartup = true).loadOnStartup()

        val ordered = inOrder(dataResetService, userService)
        ordered.verify(dataResetService).clearAll()
        ordered.verify(userService, times(fixtureLogins.size)).create(any())
    }
}
