package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a well-formed request conflicts with the current state of a resource, such as an undo after the grace
 * period. A request that breaks a rule in every state causes a [ValidationException] instead. An example is a delta
 * other than `+1`. The exception handler maps a [ConflictException] to the status 409 Conflict. The coffee app of
 * SE@UHD has the same exception.
 *
 * @param message the message of the error response
 */
class ConflictException(
    message: String
) : DomainException(message)
