package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.BootstrapAdminProperties
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [BootstrapAdminLoader] with a mocked [UserInternalService]. The loader creates an admin only when the
 * configuration has a login name, and it refuses a configuration with a login name but without the other values.
 */
class BootstrapAdminLoaderTest {
    private val userInternalService = mock<UserInternalService>()

    private fun loader(properties: BootstrapAdminProperties) = BootstrapAdminLoader(userInternalService, properties)

    private val complete =
        BootstrapAdminProperties(
            loginName = "admin",
            emailAddress = "admin@se.de",
            firstName = "Bootstrap",
            lastName = "Admin"
        )

    @Test
    fun `createBootstrapAdmin without a login name and an email address creates no admin`() {
        loader(complete.copy(loginName = " ", emailAddress = null)).createBootstrapAdmin()

        verify(userInternalService, never()).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin with an email address but without a login name throws and creates no admin`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                loader(complete.copy(loginName = null)).createBootstrapAdmin()
            }

        assertThat(exception.message).contains("campus-coffee.bootstrap-admin.login-name")
        verify(userInternalService, never()).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin with a login name that the domain refuses throws an exception with the key prefix`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(ValidationException("The login name is invalid."))

        val exception = assertThrows<IllegalArgumentException> { loader(complete).createBootstrapAdmin() }

        assertThat(exception.message).isEqualTo("campus-coffee.bootstrap-admin is invalid: The login name is invalid.")
    }

    @Test
    fun `createBootstrapAdmin after a duplicate from another instance tries again and creates no second admin`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(DuplicationException(User::class.java, "login name", "admin"))
            .thenReturn(null)

        loader(complete).createBootstrapAdmin()

        verify(userInternalService, times(2)).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin with a duplicate that stays throws an exception with the key prefix`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(DuplicationException(User::class.java, "email address", "admin@se.de"))

        val exception = assertThrows<IllegalArgumentException> { loader(complete).createBootstrapAdmin() }

        assertThat(exception.message).startsWith("campus-coffee.bootstrap-admin is invalid:")
        verify(userInternalService, times(2)).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin creates the configured user as an admin`() {
        whenever(userInternalService.createInitialAdmin(any())).thenAnswer { it.arguments[0] as User }

        loader(complete).createBootstrapAdmin()

        val created = argumentCaptor<User>()
        verify(userInternalService).createInitialAdmin(created.capture())
        assertThat(created.firstValue.loginName).isEqualTo("admin")
        assertThat(created.firstValue.emailAddress).isEqualTo("admin@se.de")
        assertThat(created.firstValue.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `createBootstrapAdmin with a login name but without an email address throws and creates no admin`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                loader(complete.copy(emailAddress = null)).createBootstrapAdmin()
            }

        assertThat(exception.message).contains("campus-coffee.bootstrap-admin.email-address")
        verify(userInternalService, never()).createInitialAdmin(any())
    }
}
