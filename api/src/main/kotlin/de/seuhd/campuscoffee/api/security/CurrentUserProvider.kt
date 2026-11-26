package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.model.User
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Returns the acting user of the current request, which [ActingUserAuthenticationFilter] stores as the principal in the
 * `SecurityContext` of Spring Security. A controller passes this [User] to the domain, which checks the role (ADR-003).
 * A controller function that calls [currentUser] must not have the annotation [OpenEndpoint], because an open endpoint
 * has no acting user. The coffee app of SE@UHD has a class with the same name and the same [currentUser].
 */
@Component
class CurrentUserProvider {
    /**
     * Returns the acting user of the current request.
     *
     * @throws AuthenticationCredentialsNotFoundException if the request has no acting user, and the client gets a 401
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
