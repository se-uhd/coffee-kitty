package de.seuhd.campuscoffee.domain.exceptions

import de.seuhd.campuscoffee.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for the log messages of [NotFoundException] and [DuplicationException]. A log message goes into the
 * server log and leaves out a looked-up or duplicate value, such as a login name.
 */
class DomainExceptionsTest {
    @Test
    fun `the log message of NotFoundException does not contain the looked-up value`() {
        val exception = NotFoundException(User::class.java, "login name", "nobody")

        assertThat(exception.logMessage).contains("User", "login name").doesNotContain("nobody")
    }

    @Test
    fun `the log message of DuplicationException does not contain the duplicate value`() {
        val exception = DuplicationException(User::class.java, "email address", "jane@se.de")

        assertThat(exception.logMessage).contains("User", "email address").doesNotContain("jane@se.de")
    }
}
