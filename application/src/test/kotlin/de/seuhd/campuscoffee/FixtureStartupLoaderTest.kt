package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.FixturesProperties
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
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
 * Unit tests for [FixtureStartupLoader] with Mockito mocks of the ports that it calls.
 */
class FixtureStartupLoaderTest {
    private val userService = mock<UserService>()
    private val userInternalService = mock<UserInternalService>()
    private val dataResetService = mock<DataResetService>()
    private val fixtureLoginNames = TestFixtures.getUserFixtures().map { it.loginName }

    /** Returns a fixture loader with `load-on-startup` true and `reset-on-startup` set to [resetOnStartup]. */
    private fun loader(resetOnStartup: Boolean = false) =
        FixtureStartupLoader(
            userService,
            userInternalService,
            dataResetService,
            FixturesProperties(loadOnStartup = true, resetOnStartup = resetOnStartup)
        )

    @Test
    fun `loadOnStartup creates the fixture users in an empty database`() {
        whenever(userService.getAll()).thenReturn(emptyList())
        whenever(userInternalService.create(any())).thenAnswer { it.arguments[0] as User }

        loader().loadOnStartup()

        val created = argumentCaptor<User>()
        verify(userInternalService, times(fixtureLoginNames.size)).create(created.capture())
        assertThat(created.allValues.map { it.loginName }).containsExactlyElementsOf(fixtureLoginNames)
        verify(dataResetService, never()).clearAll()
    }

    @Test
    fun `loadOnStartup creates no user in a database with users`() {
        whenever(userService.getAll()).thenReturn(listOf(TestFixtures.getUserFixtures().first()))

        loader().loadOnStartup()

        verify(userInternalService, never()).create(any())
        verify(dataResetService, never()).clearAll()
    }

    @Test
    fun `loadOnStartup clears the data before it creates the fixture users when reset-on-startup is true`() {
        whenever(userService.getAll()).thenReturn(listOf(TestFixtures.getUserFixtures().first()))
        whenever(userInternalService.create(any())).thenAnswer { it.arguments[0] as User }

        loader(resetOnStartup = true).loadOnStartup()

        val ordered = inOrder(dataResetService, userInternalService)
        ordered.verify(dataResetService).clearAll()
        val created = argumentCaptor<User>()
        ordered.verify(userInternalService, times(fixtureLoginNames.size)).create(created.capture())
        assertThat(created.allValues.map { it.loginName }).containsExactlyElementsOf(fixtureLoginNames)
    }
}
