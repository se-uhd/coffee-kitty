package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.model.User
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Resolves the user who acts on the current request. A controller passes this [User] to the domain, which decides
 * with the role of the user whether the action is allowed, such as `UserService.create` for a new user.
 *
 * [ActingUserAuthenticationFilter] resolves the acting user from the request header [ACTING_USER_HEADER] before
 * Spring MVC calls the controller and stores the user as the principal of the request in the `SecurityContext` of
 * Spring Security, where [currentUser] reads it. Coffee Kitty has no authentication yet, and any client can name any
 * user, an admin included. The coffee app of SE@UHD has a class with the same name and the same [currentUser], which
 * reads the user that its authentication filters stored.
 */
@Component
class CurrentUserProvider {
    /**
     * Returns the acting user of the current request.
     *
     * @throws AuthenticationCredentialsNotFoundException if the request names no acting user, which
     *   [AuthenticationExceptionHandler] answers with 401 Unauthorized
     */
    fun currentUser(): User =
        SecurityContextHolder.getContext().authentication?.principal as? User
            ?: throw AuthenticationCredentialsNotFoundException(MISSING_HEADER_MESSAGE)

    companion object {
        /** The request header that names the acting user by login name, such as `X-Acting-User: jane_doe`. */
        const val ACTING_USER_HEADER = "X-Acting-User"

        /**
         * The challenge in the header `WWW-Authenticate` of every 401 response. RFC 9110 (section 11.6.1) requires the
         * header in a 401 and defines a challenge as an authentication scheme with parameters. The scheme names the
         * request header that identifies the acting user.
         */
        const val AUTHENTICATION_CHALLENGE = "$ACTING_USER_HEADER realm=\"Coffee Kitty\""

        /** The message of a 401 for a request without the header [ACTING_USER_HEADER]. */
        const val MISSING_HEADER_MESSAGE = "The header $ACTING_USER_HEADER is missing."
    }
}
