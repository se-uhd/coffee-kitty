package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * Integration tests of `upsert` and `delete` in [CrudDataServiceImpl] against a real database. Storing a new object
 * assigns the id and both timestamps, and a violated unique constraint maps to a [DuplicationException] that names
 * the offending field through `ConstraintMapping.REGISTRY`. Deleting a user also deletes their consumption, and an
 * unknown id maps to a [NotFoundException].
 */
class UniqueConstraintDuplicationIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    private fun user(
        login: String,
        email: String = "$login@se.de"
    ): User =
        User(
            loginName = login,
            emailAddress = email,
            firstName = "First",
            lastName = "Last"
        )

    @Test
    fun `upsert of a new user assigns an id and both timestamps that getById reads back unchanged`() {
        val created = userDataService.upsert(user("alice"))

        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isEqualTo(created.createdAt)
        assertThat(userDataService.getById(requireNotNull(created.id))).isEqualTo(created)
    }

    @Test
    fun `upsert of an existing user keeps createdAt and refreshes updatedAt`() {
        val created = userDataService.upsert(user("frank"))

        val updated = userDataService.upsert(created.copy(firstName = "Franklin"))

        assertThat(updated.id).isEqualTo(created.id)
        assertThat(updated.firstName).isEqualTo("Franklin")
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)
        assertThat(userDataService.getById(requireNotNull(updated.id))).isEqualTo(updated)
    }

    @Test
    fun `upsert of a user with an unknown id throws NotFoundException`() {
        assertThatThrownBy { userDataService.upsert(user("ivy").copy(id = UUID.randomUUID())) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `a duplicate login name throws DuplicationException naming the login name in words`() {
        userDataService.upsert(user("alice"))

        assertThatThrownBy { userDataService.upsert(user("alice", email = "other@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("User with login name 'alice' already exists.")
    }

    @Test
    fun `a duplicate email address throws DuplicationException naming the email address in words`() {
        userDataService.upsert(user("bob", email = "shared@se.de"))

        assertThatThrownBy { userDataService.upsert(user("bob2", email = "shared@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("User with email address 'shared@se.de' already exists.")
    }

    @Test
    fun `a second consumption for a user throws DuplicationException naming user_id`() {
        val user = userDataService.upsert(user("erin"))
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))

        assertThatThrownBy { coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 1)) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("CoffeeConsumption with user_id 'user ${user.id}' already exists.")
    }

    @Test
    fun `upsert of an existing consumption stores the new count and leaves the user row unchanged`() {
        val user = userDataService.upsert(user("henry"))
        val userId = requireNotNull(user.id)
        val consumption = coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))
        val renamed = user.copy(firstName = "Changed")

        val updated = coffeeConsumptionDataService.upsert(consumption.copy(user = renamed, count = 1))

        assertThat(updated.count).isEqualTo(1)
        assertThat(updated.user).isEqualTo(user)
        assertThat(coffeeConsumptionDataService.getByUserId(userId).count).isEqualTo(1)
        assertThat(userDataService.getById(userId)).isEqualTo(user)
    }

    @Test
    fun `delete of a user cascades to their consumption and both lookups throw NotFoundException`() {
        val user = userDataService.upsert(user("dave"))
        val userId = requireNotNull(user.id)
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))

        userDataService.delete(userId)

        assertThatThrownBy { userDataService.getById(userId) }.isInstanceOf(NotFoundException::class.java)
        assertThatThrownBy { coffeeConsumptionDataService.getByUserId(userId) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `delete of an unknown id throws NotFoundException`() {
        assertThatThrownBy { userDataService.delete(UUID.randomUUID()) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getByLoginName returns the matching user and throws NotFoundException when none matches`() {
        userDataService.upsert(user("grace"))

        assertThat(userDataService.getByLoginName("grace").loginName).isEqualTo("grace")
        assertThatThrownBy { userDataService.getByLoginName("nobody") }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getById of an unknown id throws NotFoundException`() {
        assertThatThrownBy { userDataService.getById(UUID.randomUUID()) }
            .isInstanceOf(NotFoundException::class.java)
    }
}
