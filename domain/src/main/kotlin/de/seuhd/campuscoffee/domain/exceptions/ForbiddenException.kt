package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when the acting user is known but may not perform an action. For example, a user who is not an admin may not
 * create a user. The exception handler maps the exception to the status 403 Forbidden. The domain itself checks the
 * role of the acting user, because the check must not depend on how the api module identifies this user. The coffee app
 * of SE@UHD has the same exception.
 *
 * @param message the message of the error response
 */
class ForbiddenException(
    message: String
) : DomainException(message)
