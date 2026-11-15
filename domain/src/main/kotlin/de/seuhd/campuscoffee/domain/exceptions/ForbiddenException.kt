package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when the acting user is known but may not perform an action, such as a user who is not an admin and tries to
 * create a user. The exception handler maps it to the status 403 Forbidden. The domain decides by comparing the role
 * of the acting user with the action, so that the check does not depend on how the api module identifies the acting
 * user. The coffee app of SE@UHD has the same exception.
 *
 * @param message the message of the error response
 */
class ForbiddenException(
    message: String
) : RuntimeException(message)
