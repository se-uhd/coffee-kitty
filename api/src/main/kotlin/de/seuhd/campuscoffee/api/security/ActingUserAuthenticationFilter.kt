package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Resolves the acting user of a request before Spring MVC reads the request body. If the header
 * [CurrentUserProvider.ACTING_USER_HEADER] contains a login name, the filter stores the user with this name as the
 * principal of the request, without authorities, because the domain checks the role (ADR-003). A login name that no
 * user has gets a 401 from [entryPoint]. The filter is not a Spring bean, because Spring Boot would register a bean of
 * type `Filter` with the servlet container and run it outside the filter chain as well.
 *
 * @param userService the port that looks up the user by login name
 * @param entryPoint  the entry point that responds with 401 Unauthorized to an unknown login name
 * @param exceptionResolver the resolver that turns any other exception of the lookup into an `ErrorResponse`
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
                PreAuthenticatedAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES)
            SecurityContextHolder.setContext(context)
        }
        filterChain.doFilter(request, response)
    }

    @Suppress("TooGenericExceptionCaught")
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
            exceptionResolver.resolveException(request, response, null, e)
            null
        }

    private companion object {
        const val UNKNOWN_USER_MESSAGE = "No user has the login name in ${CurrentUserProvider.ACTING_USER_HEADER}."
    }
}
