package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.BootstrapAdminProperties
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [BootstrapAdminLoader] with a mocked [UserInternalService]. A configuration with a login name also
 * needs an email address, a first name, and a last name.
 */
class BootstrapAdminLoaderTest {
    private val userInternalService = mock<UserInternalService>()

    private fun loader(properties: BootstrapAdminProperties) = BootstrapAdminLoader(userInternalService, properties)

    @Test
    fun `createBootstrapAdmin creates no admin without a login name and an email address`() {
        loader(complete.copy(loginName = " ", emailAddress = null)).createBootstrapAdmin()

        verify(userInternalService, never()).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin throws IllegalArgumentException for an email address without a login name`() {
        assertThrows<IllegalArgumentException> { loader(complete.copy(loginName = null)).createBootstrapAdmin() }

        verify(userInternalService, never()).createInitialAdmin(any())
    }

    @Test
    fun `createBootstrapAdmin throws IllegalArgumentException for a login name that the domain rejects`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(ValidationException("The login name is invalid."))

        assertThrows<IllegalArgumentException> { loader(complete).createBootstrapAdmin() }
    }

    @Test
    fun `createBootstrapAdmin throws no exception when another instance stores the admin at the same time`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(DuplicationException(User::class.java, "login name", "admin"))
            .thenReturn(null)

        assertDoesNotThrow { loader(complete).createBootstrapAdmin() }
    }

    @Test
    fun `createBootstrapAdmin throws IllegalArgumentException for the email address of another user`() {
        whenever(userInternalService.createInitialAdmin(any()))
            .thenThrow(DuplicationException(User::class.java, "email address", "admin@se.de"))

        assertThrows<IllegalArgumentException> { loader(complete).createBootstrapAdmin() }
    }

    @ParameterizedTest
    @MethodSource("incompleteProperties")
    fun `createBootstrapAdmin throws IllegalArgumentException without an email address, first name, or last name`(
        properties: BootstrapAdminProperties
    ) {
        assertThrows<IllegalArgumentException> { loader(properties).createBootstrapAdmin() }

        verify(userInternalService, never()).createInitialAdmin(any())
    }

    private companion object {
        private val complete =
            BootstrapAdminProperties(
                loginName = "admin",
                emailAddress = "admin@se.de",
                firstName = "Bootstrap",
                lastName = "Admin"
            )

        /** Returns copies of [complete], each without a value that a configuration with a login name needs. */
        @JvmStatic
        fun incompleteProperties() =
            listOf(complete.copy(emailAddress = null), complete.copy(firstName = null), complete.copy(lastName = " "))
    }
}
