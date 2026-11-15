package de.seuhd.campuscoffee.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.json.JsonMapper

/**
 * Unit tests for [CurrentUserProvider] and [AuthenticationExceptionHandler] on a route without a rule in
 * `SecurityConfig`: without a principal, [CurrentUserProvider.currentUser] throws, and the handler turns the exception
 * into the 401 of [JsonAuthenticationEntryPoint].
 */
class AuthenticationExceptionHandlerTest {
    private val handler = AuthenticationExceptionHandler(JsonAuthenticationEntryPoint(JsonMapper.builder().build()))

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `currentUser without a principal throws, and the handler responds with 401 and the challenge`() {
        val exception = assertThrows<AuthenticationCredentialsNotFoundException> { CurrentUserProvider().currentUser() }
        val response = MockHttpServletResponse()

        handler.handleAuthenticationException(exception, MockHttpServletRequest("PUT", "/api/example"), response)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(CurrentUserProvider.AUTHENTICATION_CHALLENGE)
        assertThat(response.contentAsString)
            .contains("The header X-Acting-User is missing.", "\"path\":\"/api/example\"")
    }
}
