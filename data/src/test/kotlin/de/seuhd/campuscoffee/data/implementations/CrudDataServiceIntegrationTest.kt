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
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Integration tests of the data adapters [UserDataServiceImpl] and [CoffeeConsumptionDataServiceImpl] and their base
 * [CrudDataServiceImpl] against a real database. They check that storing an object assigns its id and timestamps and
 * that an update keeps the stored creation time. They also check the [DuplicationException] of each unique
 * constraint, the cascade from a user to their consumption, the [NotFoundException] of an unknown id, login name, or
 * user, and the lock that `getByUserIdForUpdate` takes until the transaction ends.
 */
class CrudDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private fun user(
        loginName: String,
        emailAddress: String = "$loginName@se.de"
    ): User =
        User(
            loginName = loginName,
            emailAddress = emailAddress,
            firstName = "First",
            lastName = "Last"
        )

    /** Stores a user with the given login name and their consumption at zero, and returns the user's id. */
    private fun storeUserWithConsumption(loginName: String): UUID {
        val user = userDataService.upsert(user(loginName))
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))
        return requireNotNull(user.id)
    }

    @Test
    fun `upsert of a new user assigns an id and both timestamps that getById reads back unchanged`() {
        val created = userDataService.upsert(user("alice"))

        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isEqualTo(created.createdAt)
        assertThat(userDataService.getById(requireNotNull(created.id))).isEqualTo(created)
    }

    @Test
    fun `upsert of an existing user without timestamps keeps createdAt and refreshes updatedAt`() {
        val created = userDataService.upsert(user("frank"))

        val updated = userDataService.upsert(created.copy(firstName = "Franklin", createdAt = null, updatedAt = null))

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
    fun `upsert of a user with a taken login name throws DuplicationException that names the login name`() {
        userDataService.upsert(user("alice"))

        assertThatThrownBy { userDataService.upsert(user("alice", emailAddress = "other@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("User with login name 'alice' already exists.")
    }

    @Test
    fun `upsert of a user with a taken email address throws DuplicationException that names the email address`() {
        userDataService.upsert(user("bob", emailAddress = "shared@se.de"))

        assertThatThrownBy { userDataService.upsert(user("bob2", emailAddress = "shared@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("User with email address 'shared@se.de' already exists.")
    }

    @Test
    fun `upsert of a second consumption for a user throws DuplicationException that names the user ID`() {
        val user = userDataService.upsert(user("erin"))
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))

        assertThatThrownBy { coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 1)) }
            .isInstanceOf(DuplicationException::class.java)
            .hasMessage("CoffeeConsumption with user ID '${user.id}' already exists.")
    }

    @Test
    fun `upsert of an existing consumption stores the new count and leaves createdAt and the user row unchanged`() {
        val user = userDataService.upsert(user("henry"))
        val userId = requireNotNull(user.id)
        val consumption = coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))
        val renamed = user.copy(firstName = "Changed")

        val updated =
            coffeeConsumptionDataService.upsert(
                consumption.copy(user = renamed, count = 1, createdAt = null, updatedAt = null)
            )

        assertThat(updated.count).isEqualTo(1)
        assertThat(updated.createdAt).isEqualTo(consumption.createdAt)
        assertThat(updated.user).isEqualTo(user)
        assertThat(coffeeConsumptionDataService.getByUserId(userId).count).isEqualTo(1)
        assertThat(userDataService.getById(userId)).isEqualTo(user)
    }

    @Test
    fun `upsert of a consumption for an unknown user throws NotFoundException that names the user`() {
        val unknownUser = user("nobody").copy(id = UUID(0L, 7L))

        assertThatThrownBy { coffeeConsumptionDataService.upsert(CoffeeConsumption(user = unknownUser, count = 0)) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("User with ID 00000000-0000-0000-0000-000000000007 does not exist.")
    }

    @Test
    fun `getByUserId of an unknown user throws NotFoundException that names the user`() {
        assertThatThrownBy { coffeeConsumptionDataService.getByUserId(UUID(0L, 8L)) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("User with ID 00000000-0000-0000-0000-000000000008 does not exist.")
    }

    @Test
    fun `getAll of the base class returns the consumptions in the order they were created, also after an update`() {
        val userIds = listOf("zoe", "adam", "mia").map { storeUserWithConsumption(it) }
        val first = coffeeConsumptionDataService.getByUserId(userIds.first())
        coffeeConsumptionDataService.upsert(first.copy(count = 1))

        assertThat(coffeeConsumptionDataService.getAll().map { it.user.id }).containsExactlyElementsOf(userIds)
    }

    @Test
    fun `delete of a user cascades to their consumption and both lookups throw NotFoundException`() {
        val userId = storeUserWithConsumption("dave")

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
    fun `getByLoginName returns the matching user and throws NotFoundException that names the login name`() {
        userDataService.upsert(user("grace"))

        assertThat(userDataService.getByLoginName("grace").loginName).isEqualTo("grace")
        assertThatThrownBy { userDataService.getByLoginName("nobody") }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("User with login name 'nobody' does not exist.")
    }

    @Test
    fun `getById of an unknown id throws NotFoundException`() {
        assertThatThrownBy { userDataService.getById(UUID.randomUUID()) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getByUserIdForUpdate makes a second transaction wait until the first one ends and then read its count`() {
        val userId = storeUserWithConsumption("lena")
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit {
                    transactionTemplate.executeWithoutResult {
                        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
                        coffeeConsumptionDataService.upsert(current.copy(count = 1))
                        locked.countDown()
                        release.await()
                    }
                }
            assertThat(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
            val second =
                executor.submit<Int> {
                    transactionTemplate.execute { coffeeConsumptionDataService.getByUserIdForUpdate(userId).count }
                }

            assertThatThrownBy { second.get(WAIT_MILLIS, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)
            release.countDown()
            first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(1)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `getByUserIdForUpdate outside a transaction throws IllegalTransactionStateException`() {
        val userId = storeUserWithConsumption("otto")

        assertThatThrownBy { coffeeConsumptionDataService.getByUserIdForUpdate(userId) }
            .isInstanceOf(IllegalTransactionStateException::class.java)
    }

    private companion object {
        /** How long the lock test waits before it treats the second transaction as blocked by the row lock. */
        const val WAIT_MILLIS = 500L

        /** How long the lock test waits for a step that must complete. */
        const val TIMEOUT_SECONDS = 10L
    }
}
