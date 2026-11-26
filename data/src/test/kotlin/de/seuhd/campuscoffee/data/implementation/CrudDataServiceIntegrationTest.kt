package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * The integration tests of the data adapters [UserDataServiceImpl], [CoffeeConsumptionDataServiceImpl], and
 * [CountCorrectionDataServiceImpl] and their base class [CrudDataServiceImpl] against PostgreSQL. The base class is
 * abstract, and the tests call its methods through the three data adapters.
 */
class CrudDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeConsumptionDataService: CoffeeConsumptionDataService

    @Autowired
    private lateinit var countCorrectionDataService: CountCorrectionDataService

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

    /**
     * Stores a user with [loginName] and a consumption with a coffee count of zero, and returns the id of the user.
     */
    private fun storeUserWithConsumption(loginName: String): UUID {
        val user = userDataService.upsert(user(loginName))
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))
        return requireNotNull(user.id)
    }

    /** Returns a correction of the coffee count of [user] by [admin] from 3 to 1 with [note]. */
    private fun correction(
        user: User,
        admin: User,
        note: String = "Double tap"
    ): CountCorrection = CountCorrection(user = user, admin = admin, countBefore = 3, countAfter = 1, note = note)

    /** Stores an admin with [loginName] and returns the stored admin. */
    private fun storeAdmin(loginName: String): User = userDataService.upsert(user(loginName).copy(role = Role.ADMIN))

    @Test
    fun `upsert stores a new user with an id and timestamps`() {
        val created = userDataService.upsert(user("alice"))

        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isEqualTo(created.createdAt)
        assertThat(userDataService.getById(requireNotNull(created.id))).isEqualTo(created)
    }

    @Test
    fun `upsert stores every field of an existing user`() {
        val created = userDataService.upsert(user("frank"))
        val changed =
            created.copy(
                loginName = "franklin",
                emailAddress = "franklin@se.de",
                firstName = "Franklin",
                lastName = "Changed",
                role = Role.ADMIN
            )

        val updated = userDataService.upsert(changed.copy(createdAt = null, updatedAt = null))

        assertThat(updated).isEqualTo(changed.copy(updatedAt = updated.updatedAt))
        assertThat(updated.updatedAt).isAfter(created.updatedAt)
        assertThat(userDataService.getById(requireNotNull(updated.id))).isEqualTo(updated)
    }

    @Test
    fun `upsert stores the role ADMIN`() {
        val created = userDataService.upsert(user("jane").copy(role = Role.ADMIN))

        assertThat(userDataService.getById(requireNotNull(created.id)).role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `upsert throws NotFoundException for an unknown id`() {
        assertThatThrownBy { userDataService.upsert(user("ivy").copy(id = UUID.randomUUID())) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `upsert throws DuplicationException for a taken login name`() {
        userDataService.upsert(user("alice"))

        assertThatThrownBy { userDataService.upsert(user("alice", emailAddress = "other@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
    }

    @Test
    fun `upsert throws DuplicationException for a taken email address`() {
        userDataService.upsert(user("bob", emailAddress = "shared@se.de"))

        assertThatThrownBy { userDataService.upsert(user("bob2", emailAddress = "shared@se.de")) }
            .isInstanceOf(DuplicationException::class.java)
    }

    @Test
    fun `upsert throws DuplicationException for a second consumption of a user`() {
        val user = userDataService.upsert(user("erin"))
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 0))

        assertThatThrownBy { coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 1)) }
            .isInstanceOf(DuplicationException::class.java)
    }

    @Test
    fun `upsert throws DataConstraintException with the SQL state for an email address with the NUL character`() {
        assertThatThrownBy { userDataService.upsert(user("nul", emailAddress = "nul\u0000@se.de")) }
            .isInstanceOf(DataConstraintException::class.java)
            .hasMessageContaining("22021")
        assertThat(userDataService.getAll().map { it.loginName }).doesNotContain("nul")
    }

    @Test
    fun `upsert stores a new count of a consumption`() {
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
    fun `upsert stores the coffee times oldest first, also after an update`() {
        val user = userDataService.upsert(user("ida"))
        val userId = requireNotNull(user.id)
        val first = LocalDateTime.of(2026, 11, 16, 10, 0, 0)
        val times = listOf(first, first.plusMinutes(1), first.plusMinutes(2))
        val stored = coffeeConsumptionDataService.upsert(CoffeeConsumption(user = user, count = 3, coffeeTimes = times))

        assertThat(coffeeConsumptionDataService.getByUserId(userId).coffeeTimes).containsExactlyElementsOf(times)

        val updated = times.drop(1) + first.plusMinutes(3)
        coffeeConsumptionDataService.upsert(
            stored.copy(count = 3, coffeeTimes = updated, createdAt = null, updatedAt = null)
        )

        assertThat(coffeeConsumptionDataService.getByUserId(userId).coffeeTimes).containsExactlyElementsOf(updated)
    }

    @Test
    fun `upsert throws NotFoundException for a consumption of an unknown user`() {
        val unknownUser = user("nobody").copy(id = UUID(0L, 7L))

        assertThatThrownBy { coffeeConsumptionDataService.upsert(CoffeeConsumption(user = unknownUser, count = 0)) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getAll returns the consumptions in the order of their creation, also after an update`() {
        val userIds = listOf("zoe", "adam", "mia").map { storeUserWithConsumption(it) }
        val first = coffeeConsumptionDataService.getByUserId(userIds.first())
        coffeeConsumptionDataService.upsert(first.copy(count = 1))

        assertThat(coffeeConsumptionDataService.getAll().map { it.user.id }).containsExactlyElementsOf(userIds)
    }

    @Test
    fun `getAll returns the users in the order of their login names`() {
        listOf("zoe", "adam", "mia").forEach { userDataService.upsert(user(it)) }

        assertThat(userDataService.getAll().map { it.loginName }).containsExactly("adam", "mia", "zoe")
    }

    @Test
    fun `deleting a user deletes the consumption of the user`() {
        val userId = storeUserWithConsumption("dave")

        userDataService.delete(userId)

        assertThatThrownBy { userDataService.getById(userId) }.isInstanceOf(NotFoundException::class.java)
        assertThatThrownBy { coffeeConsumptionDataService.getByUserId(userId) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `delete throws NotFoundException for an unknown id`() {
        assertThatThrownBy { userDataService.delete(UUID.randomUUID()) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getByLoginName returns the user with the same login name among users with similar login names`() {
        val grace = userDataService.upsert(user("grace"))
        userDataService.upsert(user("Grace", emailAddress = "other@se.de"))
        userDataService.upsert(user("grace_2"))

        assertThat(userDataService.getByLoginName("grace")).isEqualTo(grace)
    }

    @Test
    fun `getByLoginName throws NotFoundException for a login name in other letter case`() {
        userDataService.upsert(user("grace"))

        assertThatThrownBy { userDataService.getByLoginName("Grace") }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getByUserIdForUpdate makes a second transaction wait until the first transaction ends`() {
        val userId = storeUserWithConsumption("lena")
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit {
                    transactionTemplate.executeWithoutResult {
                        val current = coffeeConsumptionDataService.getByUserIdForUpdate(userId)
                        locked.countDown()
                        release.await()
                        coffeeConsumptionDataService.upsert(current.copy(count = 1))
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
    fun `insert stores a new correction with an id, timestamps, the user, and the admin`() {
        val user = userDataService.upsert(user("paula"))
        val admin = storeAdmin("quentin")

        val created = countCorrectionDataService.insert(correction(user, admin))

        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isEqualTo(created.createdAt)
        assertThat(created.user).isEqualTo(user)
        assertThat(created.admin).isEqualTo(admin)
        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).containsExactly(created)
    }

    @Test
    fun `insert throws IllegalArgumentException for a stored correction`() {
        val user = userDataService.upsert(user("rosa"))
        val created = countCorrectionDataService.insert(correction(user, storeAdmin("sam")))

        assertThatThrownBy { countCorrectionDataService.insert(created.copy(note = "Typo")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).containsExactly(created)
    }

    @ParameterizedTest
    @ValueSource(strings = ["countBefore", "countAfter", "note"])
    fun `insert throws DataConstraintException for a correction with a negative count or an empty note`(field: String) {
        val user = userDataService.upsert(user("olaf"))
        val correction = correction(user, storeAdmin("nina"))
        val invalid =
            when (field) {
                "countBefore" -> correction.copy(countBefore = -1)
                "countAfter" -> correction.copy(countAfter = -1)
                else -> correction.copy(note = "")
            }

        assertThatThrownBy { countCorrectionDataService.insert(invalid) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).isEmpty()
    }

    @Test
    fun `getAllByUserId returns the corrections of one user, newest first`() {
        val user = userDataService.upsert(user("tina"))
        val otherUser = userDataService.upsert(user("uwe"))
        val admin = storeAdmin("vera")
        val first = countCorrectionDataService.insert(correction(user, admin, note = "First"))
        countCorrectionDataService.insert(correction(otherUser, admin, note = "Other user"))
        val second = countCorrectionDataService.insert(correction(user, admin, note = "Second"))

        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).containsExactly(second, first)
    }

    @Test
    fun `deleting a user deletes the corrections of the user`() {
        val user = userDataService.upsert(user("walter"))
        val admin = storeAdmin("xenia")
        countCorrectionDataService.insert(correction(user, admin))

        userDataService.delete(requireNotNull(user.id))

        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).isEmpty()
    }

    @Test
    fun `deleting an admin who corrected another coffee count throws DataConstraintException`() {
        val user = userDataService.upsert(user("yusuf"))
        val admin = storeAdmin("zora")
        countCorrectionDataService.insert(correction(user, admin))

        // the foreign key admin_id has the default NO ACTION, because a correction does not belong to its admin
        assertThatThrownBy { userDataService.delete(requireNotNull(admin.id)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(countCorrectionDataService.getAllByUserId(requireNotNull(user.id))).hasSize(1)
    }

    private companion object {
        /**
         * The number of milliseconds for which the row lock of `getByUserIdForUpdate` must block a transaction. In 34
         * runs on 2026-11-25, the second transaction needed at most 35 milliseconds once the lock was released. A
         * transaction that is still waiting after 500 milliseconds is therefore blocked and not only slow.
         */
        const val WAIT_MILLIS = 500L

        /**
         * The maximum number of seconds that the test of `getByUserIdForUpdate` waits for a step that must finish.
         * Only a step that hangs reaches the limit: The steps together took at most 35 milliseconds (see
         * [WAIT_MILLIS]).
         */
        const val TIMEOUT_SECONDS = 10L
    }
}
