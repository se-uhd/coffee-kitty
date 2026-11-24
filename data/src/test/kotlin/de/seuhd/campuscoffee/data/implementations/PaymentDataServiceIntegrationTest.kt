package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.PaymentDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * Integration tests of [PaymentDataServiceImpl] against PostgreSQL. A payment with a user is a deposit, and a payment
 * without a user is a kitty adjustment.
 */
class PaymentDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var paymentDataService: PaymentDataService

    @Autowired
    private lateinit var userDataService: UserDataService

    /** Returns a user with [loginName] that is not stored yet. */
    private fun user(loginName: String): User =
        User(loginName = loginName, emailAddress = "$loginName@se.de", firstName = "First", lastName = "Last")

    /** Stores a user with [loginName] and returns the stored user. */
    private fun storeUser(loginName: String): User = userDataService.upsert(user(loginName))

    @Test
    fun `upsert stores a deposit with a user and a kitty adjustment without a user`() {
        val user = storeUser("alice")
        val longNote = "Cash".padEnd(Payment.MAX_NOTE_LENGTH, '.')

        val deposit = paymentDataService.upsert(Payment(user = user, amountCents = 1000, note = longNote))
        val adjustment = paymentDataService.upsert(Payment(amountCents = -250))

        assertThat(deposit.id).isNotNull()
        assertThat(paymentDataService.getAll()).containsExactly(deposit, adjustment)
        assertThat(deposit)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(Payment(user = user, amountCents = 1000, note = longNote))
        assertThat(adjustment)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(Payment(amountCents = -250))
    }

    @Test
    fun `getAllByUserId returns the deposits of one user, newest first, without kitty adjustments`() {
        val user = storeUser("bob")
        val otherUser = storeUser("carla")
        val first = paymentDataService.upsert(Payment(user = user, amountCents = 500))
        paymentDataService.upsert(Payment(user = otherUser, amountCents = 700))
        paymentDataService.upsert(Payment(amountCents = 2000))
        val second = paymentDataService.upsert(Payment(user = user, amountCents = 300))

        assertThat(paymentDataService.getAllByUserId(requireNotNull(user.id))).containsExactly(second, first)
    }

    @Test
    fun `upsert of a stored deposit changes neither createdAt nor the user`() {
        val user = storeUser("dora")
        val created = paymentDataService.upsert(Payment(user = user, amountCents = 500))

        val updated =
            paymentDataService.upsert(
                created.copy(user = user.copy(firstName = "Changed"), note = "Typo", createdAt = null, updatedAt = null)
            )

        assertThat(updated.note).isEqualTo("Typo")
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(userDataService.getById(requireNotNull(user.id))).isEqualTo(user)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `upsert throws DataConstraintException for a deposit of 0 or -1`(amountCents: Int) {
        val user = storeUser("emil")

        assertThatThrownBy { paymentDataService.upsert(Payment(user = user, amountCents = amountCents)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(paymentDataService.getAll()).isEmpty()
    }

    @Test
    fun `upsert throws DataConstraintException for a kitty adjustment of zero`() {
        assertThatThrownBy { paymentDataService.upsert(Payment(amountCents = 0)) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `upsert throws DataConstraintException for a payment with an empty note`() {
        assertThatThrownBy { paymentDataService.upsert(Payment(amountCents = 100, note = "")) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `upsert throws NotFoundException for a deposit of an unknown user`() {
        val unknownUser = user("nobody").copy(id = UUID(0L, 9L))

        assertThatThrownBy { paymentDataService.upsert(Payment(user = unknownUser, amountCents = 100)) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `upsert throws IllegalArgumentException for a deposit of a user without an id`() {
        // without the check, the entity mapper would map the user to null, and upsert would store a kitty adjustment
        assertThatThrownBy { paymentDataService.upsert(Payment(user = user("gustav"), amountCents = 500)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(paymentDataService.getAll()).isEmpty()
    }

    @Test
    fun `deleting a user with a deposit throws DataConstraintException`() {
        val user = storeUser("frida")
        paymentDataService.upsert(Payment(user = user, amountCents = 500))

        // the foreign key user_id has the default NO ACTION, because a deposit is money in the kitty
        assertThatThrownBy { userDataService.delete(requireNotNull(user.id)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(paymentDataService.getAll()).hasSize(1)
    }
}
