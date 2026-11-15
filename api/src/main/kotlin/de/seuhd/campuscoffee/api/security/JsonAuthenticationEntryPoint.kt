package de.seuhd.campuscoffee.api.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * Responds to a request without a known acting user with 401 Unauthorized and the same JSON `ErrorResponse` as the
 * other error responses, with the error code `Unauthorized`. As RFC 9110 requires for a 401, the response has the
 * header `WWW-Authenticate` with the challenge [CurrentUserProvider.AUTHENTICATION_CHALLENGE].
 *
 * Spring Security calls the entry point for a route that `SecurityConfig` protects when the request has no acting user.
 * [ActingUserAuthenticationFilter] calls it for a login name that no user has, and [AuthenticationExceptionHandler] for
 * a controller that needs an acting user on a route that `SecurityConfig` does not protect. The coffee app of SE@UHD
 * has an entry point with the same name.
 *
 * @param jsonMapper the JSON mapper of Spring MVC, which writes the timestamp in the same format as for the other
 *   error responses
 */
@Component
class JsonAuthenticationEntryPoint(
    private val jsonMapper: JsonMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val message =
            if (authException is InsufficientAuthenticationException) {
                CurrentUserProvider.MISSING_HEADER_MESSAGE
            } else {
                authException.message ?: CurrentUserProvider.MISSING_HEADER_MESSAGE
            }
        log.info { "Rejected ${request.method} ${request.requestURI} with 401: $message" }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, CurrentUserProvider.AUTHENTICATION_CHALLENGE)
        response.writeErrorResponse(jsonMapper, HttpStatus.UNAUTHORIZED, message, request.requestURI)
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
