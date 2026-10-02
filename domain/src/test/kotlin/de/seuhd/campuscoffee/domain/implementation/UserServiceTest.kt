package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.reflect.jvm.javaMethod

/**
 * Unit tests for [UserServiceImpl], mocking the user and coffee consumption data ports, which echo what they store.
 * The user port assigns [createdId] to a new user, as the real store assigns an id.
 */
class UserServiceTest {
    private val userDataService: UserDataService = mock()
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService = mock()
    private val service = UserServiceImpl(userDataService, coffeeConsumptionDataService)

    private val userId: UUID = UUID(0L, 1L)
    private val createdId: UUID = UUID(0L, 2L)

    private val storedUser =
        User(
            id = userId,
            loginName = "max",
            emailAddress = "max@se.de",
            firstName = "Max",
            lastName = "M"
        )

    /** A new user with [loginName] and [emailAddress], as a request or a fixture carries it. */
    private fun newUser(
        loginName: String = "fresh",
        emailAddress: String = "fresh@se.de"
    ) = User(loginName = loginName, emailAddress = emailAddress, firstName = "Fresh", lastName = "User")

    /** Makes both data ports echo what they store, with [createdId] assigned to a new user. */
    private fun stubUpserts() {
        whenever(userDataService.upsert(any())).thenAnswer { (it.arguments[0] as User).copy(id = createdId) }
        whenever(coffeeConsumptionDataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }
    }

    /** Asserts that neither data port stored anything. */
    private fun verifyNothingStored() {
        verify(userDataService, never()).upsert(any())
        verify(coffeeConsumptionDataService, never()).upsert(any())
    }

    @Test
    fun `getAll returns every user from the data service`() {
        whenever(userDataService.getAll()).thenReturn(listOf(storedUser))

        assertThat(service.getAll()).containsExactly(storedUser)
    }

    @Test
    fun `getById returns the user from the data service`() {
        whenever(userDataService.getById(userId)).thenReturn(storedUser)

        assertThat(service.getById(userId)).isEqualTo(storedUser)
    }

    @Test
    fun `getById of an unknown id throws NotFoundException`() {
        whenever(userDataService.getById(userId)).thenThrow(NotFoundException(User::class.java, userId))

        assertThrows<NotFoundException> { service.getById(userId) }
    }

    @Test
    fun `create stores the user with a null id and the normalized email address and then a consumption at zero`() {
        stubUpserts()

        val created = service.create(newUser(emailAddress = "  Fresh@SE.de ").copy(id = UUID(0L, 7L)))

        assertThat(created.id).isEqualTo(createdId)
        assertThat(created.emailAddress).isEqualTo("fresh@se.de")
        inOrder(userDataService, coffeeConsumptionDataService) {
            verify(userDataService).upsert(newUser(emailAddress = "fresh@se.de"))
            verify(coffeeConsumptionDataService).upsert(CoffeeConsumption(user = created, count = 0))
        }
    }

    @Test
    fun `create is transactional with REQUIRED propagation`() {
        val create = requireNotNull(UserServiceImpl::create.javaMethod)

        // REQUIRED joins or opens a transaction, so a failure to store the consumption rolls the new user back
        assertThat(create.getAnnotation(Transactional::class.java)?.propagation).isEqualTo(Propagation.REQUIRED)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "s.baltes", "jane doe", "jäne"])
    fun `create with a malformed login name throws ValidationException and stores nothing`(loginName: String) {
        assertThrows<ValidationException> { service.create(newUser(loginName = loginName)) }
        verifyNothingStored()
    }

    @Test
    fun `create with a login name of 256 characters throws ValidationException and stores nothing`() {
        assertThrows<ValidationException> { service.create(newUser(loginName = "a".repeat(256))) }
        verifyNothingStored()
    }

    @Test
    fun `create with a login name of 255 characters stores the user`() {
        stubUpserts()
        val loginName = "a".repeat(255)

        service.create(newUser(loginName = loginName))

        verify(userDataService).upsert(newUser(loginName = loginName))
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-at-sign.de", "jane doe@se.de", "jane@@se.de", "@se.de", "jane@", "   "])
    fun `create with an email address other than name@domain throws ValidationException and stores nothing`(
        emailAddress: String
    ) {
        assertThrows<ValidationException> { service.create(newUser(emailAddress = emailAddress)) }
        verifyNothingStored()
    }

    @Test
    fun `create with an email address of 255 characters throws ValidationException and stores nothing`() {
        assertThrows<ValidationException> { service.create(newUser(emailAddress = "a".repeat(249) + "@se.de")) }
        verifyNothingStored()
    }
}
