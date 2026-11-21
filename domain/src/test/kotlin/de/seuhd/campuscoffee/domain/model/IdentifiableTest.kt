package de.seuhd.campuscoffee.domain.model

import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

/**
 * Unit tests for the extension property [persistedId], with a [User] as the object that has or lacks an id. A fixture
 * user from [TestFixtures] has no id, because it is not stored.
 */
class IdentifiableTest {
    private val userWithoutId = TestFixtures.regularUser()

    @Test
    fun `persistedId of a stored user returns the id of the user`() {
        val id = UUID(0L, 1L)

        assertThat(userWithoutId.copy(id = id).persistedId).isEqualTo(id)
    }

    @Test
    fun `persistedId of a user without an id throws IllegalStateException that names the class`() {
        val exception = assertThrows<IllegalStateException> { userWithoutId.persistedId }

        assertThat(exception.message).isEqualTo("User has no id, because it is not stored yet.")
    }
}
