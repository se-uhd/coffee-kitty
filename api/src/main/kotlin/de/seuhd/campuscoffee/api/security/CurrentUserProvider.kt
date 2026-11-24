package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.model.User
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Returns the acting user of the current request. A controller passes this [User] to the domain, which checks whether
 * the role of the user allows the action. `UserService.create`, for example, requires an admin.
 *
 * [ActingUserAuthenticationFilter] resolves the acting user from the request header [ACTING_USER_HEADER] before
 * any controller runs. The filter stores the user as the principal of the request in the `SecurityContext` of
 * Spring Security, where [currentUser] reads it. Coffee Kitty has no authentication yet, and any client can act as any
 * user, including an admin. The coffee app of SE@UHD has a class with the same name and the same [currentUser]. In that
 * app, [currentUser] returns the user that the authentication filters stored.
 */
@Component
class CurrentUserProvider {
    /**
     * Returns the acting user of the current request.
     *
     * @throws AuthenticationCredentialsNotFoundException if the request has no acting user.
     *   [AuthenticationExceptionHandler] turns this exception into a 401 Unauthorized.
     */
    fun currentUser(): User =
        SecurityContextHolder.getContext().authentication?.principal as? User
            ?: throw AuthenticationCredentialsNotFoundException(MISSING_HEADER_MESSAGE)

    companion object {
        /** The request header with the login name of the acting user, such as `X-Acting-User: jane_doe`. */
        const val ACTING_USER_HEADER = "X-Acting-User"

        /**
         * The challenge in the header `WWW-Authenticate` of every 401 response. RFC 9110 (section 11.6.1) requires the
         * header in a 401 and defines a challenge as an authentication scheme with parameters. The scheme is the name
         * of the request header that identifies the acting user.
         */
        const val AUTHENTICATION_CHALLENGE = "$ACTING_USER_HEADER realm=\"Coffee Kitty\""

        /** The message of a 401 for a request without the header [ACTING_USER_HEADER] or with a blank value in it. */
        const val MISSING_HEADER_MESSAGE = "The header $ACTING_USER_HEADER is missing or blank."
    }
}
