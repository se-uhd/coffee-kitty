package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for [requireAdmin], the role check that a domain service runs before an action of an admin. The function
 * depends on no port, so the tests need no mock. The admin and the user who is not an admin are fixture users from
 * [TestFixtures].
 */
class AuthorizationUtilTest {
    @Test
    fun `requireAdmin by an admin throws no exception`() {
        assertDoesNotThrow { requireAdmin(TestFixtures.admin(), "correct a coffee count") }
    }

    @Test
    fun `requireAdmin by a user who is not an admin throws ForbiddenException that names the action`() {
        val exception =
            assertThrows<ForbiddenException> { requireAdmin(TestFixtures.regularUser(), "correct a coffee count") }

        assertThat(exception.message).isEqualTo("Only an admin may correct a coffee count.")
    }
}
