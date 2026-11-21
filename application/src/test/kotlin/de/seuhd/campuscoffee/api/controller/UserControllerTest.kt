package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.mapper.UserDtoMapperImpl
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

/**
 * Unit tests for [UserController] without a Spring context. Mockito mocks replace [UserService] and
 * [CurrentUserProvider], and the DTO mapper is the `UserDtoMapperImpl` that MapStruct generates. The tests use the
 * mocks in three ways. A stub returns a fixed value (`whenever`), a verification checks whether the controller called
 * the service (`verify`), and an argument captor records the arguments of a call (`argumentCaptor`).
 *
 * [setUp] prepares the state that the controller needs before each test: the request from which `create` builds the
 * `Location` header, and the acting user. Spring keeps the current request per thread, so [tearDown] removes it after
 * each test.
 */
class UserControllerTest {
    private val userService: UserService = mock()
    private val currentUserProvider: CurrentUserProvider = mock()
    private val controller = UserController(userService, UserDtoMapperImpl(), currentUserProvider)

    private val admin = TestFixtures.admin()
    private val storedId: UUID = UUID(0L, 1L)

    private val newUserDto =
        UserDto(
            loginName = "new_user",
            emailAddress = "new.user@se.de",
            firstName = "New",
            lastName = "User"
        )

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest("POST", "/api/users")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        whenever(currentUserProvider.currentUser()).thenReturn(admin)
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    /** Stubs `create` of the service to return the new user with the id [storedId]. */
    private fun stubCreate() {
        whenever(userService.create(any(), any())).thenAnswer { (it.arguments[0] as User).copy(id = storedId) }
    }

    @Test
    fun `getById returns 200 OK with the DTO of the user that the stubbed service returns`() {
        whenever(userService.getById(storedId)).thenReturn(TestFixtures.regularUser().copy(id = storedId))

        val response = controller.getById(storedId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.loginName).isEqualTo("maxmustermann")
    }

    @Test
    fun `create with an id in the body throws ValidationException and never calls the mocked service`() {
        assertThrows<ValidationException> { controller.create(newUserDto.copy(id = storedId)) }

        verify(userService, never()).create(any(), any())
    }

    @Test
    fun `create passes the user of the request body and the acting user to the service, as the captors record`() {
        stubCreate()
        val user = argumentCaptor<User>()
        val actingUser = argumentCaptor<User>()

        controller.create(newUserDto)

        verify(userService).create(user.capture(), actingUser.capture())
        assertThat(user.firstValue.loginName).isEqualTo("new_user")
        assertThat(user.firstValue.role).isEqualTo(Role.USER)
        assertThat(actingUser.firstValue).isEqualTo(admin)
    }

    @Test
    fun `create returns 201 Created with the URL of the new user in the Location header`() {
        stubCreate()

        val response = controller.create(newUserDto)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).hasToString("http://localhost/api/users/$storedId")
    }
}
