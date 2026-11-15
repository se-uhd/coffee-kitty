package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a well-formed request conflicts with the current state of a resource, such as an undo after the grace
 * period. Unlike the malformed input of a [ValidationException], such as a delta other than `+1`, the request
 * is valid in itself but cannot apply to the current state. The exception handler maps it to the status 409 Conflict.
 * The coffee app of SE@UHD has the same exception.
 *
 * @param message the message of the error response
 */
class ConflictException(
    message: String
) : RuntimeException(message)
