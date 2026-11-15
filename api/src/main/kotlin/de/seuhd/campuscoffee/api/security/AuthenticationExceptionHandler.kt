package de.seuhd.campuscoffee.api.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Responds with the 401 of [JsonAuthenticationEntryPoint] when a controller throws an [AuthenticationException].
 * [CurrentUserProvider.currentUser] throws such an exception when a route that `SecurityConfig` does not protect needs
 * an acting user and the request has none. The advice has the highest precedence, because `GlobalExceptionHandler`
 * would otherwise turn the exception into a 500.
 *
 * @param entryPoint the entry point that writes the 401
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthenticationExceptionHandler(
    private val entryPoint: JsonAuthenticationEntryPoint
) {
    /**
     * Writes the 401 of [entryPoint] for [exception].
     *
     * @param exception the exception that the controller threw
     * @param request   the current request
     * @param response  the response that [entryPoint] writes
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        exception: AuthenticationException,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) = entryPoint.commence(request, response, exception)
}
