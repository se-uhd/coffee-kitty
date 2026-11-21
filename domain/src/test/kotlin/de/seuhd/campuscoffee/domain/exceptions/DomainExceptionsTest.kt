package de.seuhd.campuscoffee.domain.exceptions

import de.seuhd.campuscoffee.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for the messages of [NotFoundException] and [DuplicationException]. The message of a domain exception
 * goes into the error response. The log message goes into the server log and leaves out a looked-up or duplicate
 * value, such as a login name.
 */
class DomainExceptionsTest {
    @Test
    fun `NotFoundException for an id names the class and the id in its message`() {
        val exception = NotFoundException(User::class.java, UUID(0L, 1L))

        assertThat(exception.message).isEqualTo("User with ID 00000000-0000-0000-0000-000000000001 does not exist.")
    }

    @Test
    fun `NotFoundException for a field value names the value in its message but not in its log message`() {
        val exception = NotFoundException(User::class.java, "login name", "nobody")

        assertThat(exception.message).isEqualTo("User with login name 'nobody' does not exist.")
        assertThat(exception.logMessage).isEqualTo("User with this login name does not exist.")
    }

    @Test
    fun `DuplicationException names the duplicate value in its message but not in its log message`() {
        val exception = DuplicationException(User::class.java, "email address", "jane@se.de")

        assertThat(exception.message).isEqualTo("User with email address 'jane@se.de' already exists.")
        assertThat(exception.logMessage).doesNotContain("jane@se.de")
    }
}
