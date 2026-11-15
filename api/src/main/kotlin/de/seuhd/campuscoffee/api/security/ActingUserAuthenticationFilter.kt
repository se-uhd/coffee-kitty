package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Resolves the acting user of a request before Spring MVC calls a controller and reads the request body. If the request
 * names a login name in the header [CurrentUserProvider.ACTING_USER_HEADER], the filter looks up the user and stores
 * the user as the principal of the request, with the authority `ROLE_USER` or `ROLE_ADMIN`. Spring Security then
 * treats the request as authenticated, although Coffee Kitty checks no password or token and any client can name any
 * user. A login name that no user has gets a 401 from [entryPoint], and the request does not reach a controller. A
 * request without the header continues without a principal, and `SecurityConfig` decides whether its route needs one.
 * Any other exception of the lookup, such as a failed database connection, goes to [exceptionResolver], which answers
 * it like an exception of a controller.
 *
 * `SecurityConfig` creates the filter for its filter chain. The filter is not a Spring bean, because Spring Boot would
 * register a bean of type `Filter` a second time for every request, outside the filter chain.
 *
 * @param userService the port that looks up the user by login name
 * @param entryPoint  the entry point that answers an unknown login name with 401 Unauthorized
 * @param exceptionResolver the resolver of Spring MVC that answers any other exception with an `ErrorResponse`
 */
class ActingUserAuthenticationFilter(
    private val userService: UserService,
    private val entryPoint: AuthenticationEntryPoint,
    private val exceptionResolver: HandlerExceptionResolver
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val loginName = request.getHeader(CurrentUserProvider.ACTING_USER_HEADER)?.trim()
        if (!loginName.isNullOrEmpty()) {
            val user = lookUp(loginName, request, response) ?: return
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication =
                PreAuthenticatedAuthenticationToken(
                    user,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                )
            SecurityContextHolder.setContext(context)
        }
        filterChain.doFilter(request, response)
    }

    /**
     * Returns the user with [loginName], or null after the filter has answered the request, because no user has
     * [loginName] or the lookup failed.
     */
    @Suppress("TooGenericExceptionCaught") // any failure of the lookup has to become an error response
    private fun lookUp(
        loginName: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): User? =
        try {
            userService.getByLoginName(loginName)
        } catch (_: NotFoundException) {
            SecurityContextHolder.clearContext()
            entryPoint.commence(request, response, BadCredentialsException(UNKNOWN_USER_MESSAGE))
            null
        } catch (e: RuntimeException) {
            // GlobalExceptionHandler answers any other failure, so that the body is an ErrorResponse
            exceptionResolver.resolveException(request, response, null, e)
            null
        }

    private companion object {
        const val UNKNOWN_USER_MESSAGE = "No user has the login name in ${CurrentUserProvider.ACTING_USER_HEADER}."
    }
}
