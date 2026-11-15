package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver
import tools.jackson.databind.json.JsonMapper

/**
 * Unit tests for [ActingUserAuthenticationFilter] with a mocked [UserService] and the real
 * [JsonAuthenticationEntryPoint]. The filter stores a known acting user as the principal, answers an unknown login name
 * with 401, and passes any other exception of the lookup to the exception resolver of Spring MVC.
 */
class ActingUserAuthenticationFilterTest {
    private val userService = mock<UserService>()
    private val exceptionResolver = mock<HandlerExceptionResolver>()
    private val filter =
        ActingUserAuthenticationFilter(
            userService,
            JsonAuthenticationEntryPoint(JsonMapper.builder().build()),
            exceptionResolver
        )
    private val admin =
        User(
            loginName = "jane_doe",
            emailAddress = "jane@se.de",
            firstName = "Jane",
            lastName = "Doe",
            role = Role.ADMIN
        )

    /** Returns a request to `POST /api/users` with [loginName] in the header `X-Acting-User`. */
    private fun request(loginName: String) =
        MockHttpServletRequest("POST", "/api/users").apply {
            addHeader(CurrentUserProvider.ACTING_USER_HEADER, loginName)
        }

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `a known login name becomes the principal of the request`() {
        whenever(userService.getByLoginName("jane_doe")).thenReturn(admin)
        var principal: Any? = null
        val chain = FilterChain { _, _ -> principal = SecurityContextHolder.getContext().authentication?.principal }

        filter.doFilter(request(" jane_doe "), MockHttpServletResponse(), chain)

        assertThat(principal).isEqualTo(admin)
    }

    @Test
    fun `an unknown login name gets 401 with the challenge and does not reach the controller`() {
        whenever(userService.getByLoginName("nobody"))
            .thenThrow(NotFoundException(User::class.java, "login name", "nobody"))
        val response = MockHttpServletResponse()
        var called = false

        filter.doFilter(request("nobody"), response, FilterChain { _, _ -> called = true })

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(CurrentUserProvider.AUTHENTICATION_CHALLENGE)
        assertThat(response.contentAsString).contains("No user has the login name in X-Acting-User.")
        assertThat(called).isFalse()
    }

    @Test
    fun `another exception of the lookup goes to the exception resolver and does not reach the controller`() {
        val failure = IllegalStateException("The database is not available.")
        whenever(userService.getByLoginName("jane_doe")).thenThrow(failure)
        var called = false

        filter.doFilter(request("jane_doe"), MockHttpServletResponse(), FilterChain { _, _ -> called = true })

        verify(exceptionResolver).resolveException(any(), any(), anyOrNull(), eq(failure))
        assertThat(called).isFalse()
    }
}
