package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

/**
 * The unit tests for [UserServiceImpl] with Mockito mocks of [UserDataService] and [CoffeeConsumptionDataService].
 */
class UserServiceTest {
    private val userDataService: UserDataService = mock()
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService = mock()
    private val service = UserServiceImpl(userDataService, coffeeConsumptionDataService, domainValidator())

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

    /** Returns a user with [loginName] and [emailAddress] and without an id or timestamps. */
    private fun newUser(
        loginName: String = "fresh",
        emailAddress: String = "fresh@se.de"
    ) = User(loginName = loginName, emailAddress = emailAddress, firstName = "Fresh", lastName = "User")

    /**
     * Stubs `upsert` on both mocks to return its argument. The mock of [UserDataService] returns the user with the id
     * [createdId].
     */
    private fun stubUpserts() {
        whenever(userDataService.upsert(any())).thenAnswer { (it.arguments[0] as User).copy(id = createdId) }
        whenever(coffeeConsumptionDataService.upsert(any())).thenAnswer { it.arguments[0] as CoffeeConsumption }
    }

    /** Asserts that the service stored neither a user nor a consumption. */
    private fun verifyNothingStored() {
        verify(userDataService, never()).upsert(any())
        verify(coffeeConsumptionDataService, never()).upsert(any())
    }

    @Test
    fun `getAll returns the users of the data port`() {
        whenever(userDataService.getAll()).thenReturn(listOf(storedUser))

        assertThat(service.getAll()).containsExactly(storedUser)
    }

    @Test
    fun `create stores a trimmed lowercase email address and ignores the id and the timestamps`() {
        stubUpserts()
        val clientTime = LocalDateTime.of(2000, 1, 1, 0, 0)

        val created =
            service.create(
                newUser(emailAddress = "  Fresh@SE.de ")
                    .copy(id = UUID(0L, 7L), createdAt = clientTime, updatedAt = clientTime)
            )

        assertThat(created.id).isEqualTo(createdId)
        assertThat(created.emailAddress).isEqualTo("fresh@se.de")
        verify(userDataService).upsert(newUser(emailAddress = "fresh@se.de"))
        verify(coffeeConsumptionDataService).upsert(CoffeeConsumption(user = created, count = 0))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "s.baltes", "jane doe", "jäne"])
    fun `create throws ValidationException for a malformed login name`(loginName: String) {
        assertThrows<ValidationException> { service.create(newUser(loginName = loginName)) }
        verifyNothingStored()
    }

    @Test
    fun `create stores a login name of one character`() {
        stubUpserts()

        service.create(newUser(loginName = "a"))

        verify(userDataService).upsert(newUser(loginName = "a"))
    }

    @Test
    fun `create throws ValidationException for a login name of 256 characters`() {
        assertThrows<ValidationException> { service.create(newUser(loginName = "a".repeat(256))) }
        verifyNothingStored()
    }

    @Test
    fun `create stores a login name of 255 characters`() {
        stubUpserts()
        val loginName = "a".repeat(255)

        service.create(newUser(loginName = loginName))

        verify(userDataService).upsert(newUser(loginName = loginName))
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-at-sign.de", "jane doe@se.de", "jane@@se.de", "@se.de", "jane@", "jane@se de", "   "])
    fun `create throws ValidationException for an email address without the form name@domain`(emailAddress: String) {
        assertThrows<ValidationException> { service.create(newUser(emailAddress = emailAddress)) }
        verifyNothingStored()
    }

    @ParameterizedTest
    @ValueSource(strings = ["ja\u0000ne@se.de", "jane@se.de\u0000", "ja\u0001ne@se.de", "ja\u007Fne@se.de"])
    fun `create throws ValidationException for an email address with a control character`(emailAddress: String) {
        assertThrows<ValidationException> { service.create(newUser(emailAddress = emailAddress)) }
        verifyNothingStored()
    }

    @Test
    fun `create throws ValidationException for an email address of 255 characters`() {
        assertThrows<ValidationException> { service.create(newUser(emailAddress = "a".repeat(249) + "@se.de")) }
        verifyNothingStored()
    }

    @Test
    fun `create stores an email address of 254 characters`() {
        stubUpserts()
        val emailAddress = "a".repeat(248) + "@se.de"

        service.create(newUser(emailAddress = emailAddress))

        verify(userDataService).upsert(newUser(emailAddress = emailAddress))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\u00A0", "Fre\u0000sh", "Fre\nsh"])
    fun `create throws ValidationException for a first name that is blank or contains a control character`(
        firstName: String
    ) {
        assertThrows<ValidationException> { service.create(newUser().copy(firstName = firstName)) }
        verifyNothingStored()
    }

    @Test
    fun `create throws ValidationException for a last name of 256 characters`() {
        assertThrows<ValidationException> { service.create(newUser().copy(lastName = "a".repeat(256))) }
        verifyNothingStored()
    }

    @Test
    fun `create stores a last name of 255 characters`() {
        stubUpserts()
        val user = newUser().copy(lastName = "a".repeat(255))

        service.create(user)

        verify(userDataService).upsert(user)
    }

    @Test
    fun `createInitialAdmin stores an admin and a consumption when no admin exists`() {
        stubUpserts()
        whenever(userDataService.getAll()).thenReturn(listOf(storedUser))

        val created = requireNotNull(service.createInitialAdmin(newUser()))

        val admin = newUser().copy(role = Role.ADMIN)
        assertThat(created).isEqualTo(admin.copy(id = createdId))
        verify(userDataService).upsert(admin)
        verify(coffeeConsumptionDataService).upsert(CoffeeConsumption(user = created, count = 0))
    }

    @Test
    fun `createInitialAdmin gives the role ADMIN to a stored user with the login name of the admin`() {
        whenever(userDataService.getAll()).thenReturn(listOf(storedUser))
        whenever(userDataService.upsert(any())).thenAnswer { it.arguments[0] as User }

        val promoted = service.createInitialAdmin(newUser(loginName = storedUser.loginName))

        assertThat(promoted).isEqualTo(storedUser.copy(role = Role.ADMIN))
        verify(userDataService).upsert(storedUser.copy(role = Role.ADMIN))
        verify(coffeeConsumptionDataService, never()).upsert(any())
    }

    @Test
    fun `createInitialAdmin returns null when an admin exists`() {
        val existingAdmin = newUser().copy(id = createdId, role = Role.ADMIN)
        whenever(userDataService.getAll()).thenReturn(listOf(storedUser, existingAdmin))

        assertThat(service.createInitialAdmin(newUser(loginName = storedUser.loginName))).isNull()
        verifyNothingStored()
    }

    @Test
    fun `create by an admin stores the user and a consumption with a count of zero`() {
        stubUpserts()
        val admin = storedUser.copy(role = Role.ADMIN)

        val created = service.create(newUser(), admin)

        assertThat(created).isEqualTo(newUser().copy(id = createdId))
        verify(userDataService).upsert(newUser())
        verify(coffeeConsumptionDataService).upsert(CoffeeConsumption(user = created, count = 0))
    }

    @Test
    fun `create throws ForbiddenException for a user who is not an admin`() {
        assertThrows<ForbiddenException> { service.create(newUser(), storedUser) }

        verifyNothingStored()
    }
}
