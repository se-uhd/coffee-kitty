package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a change is based on a value that changed since the client read it, such as a correction of a coffee
 * count that a coffee changed meanwhile (FR-8). Unlike a [ConflictException], it tells the client to read the current
 * value and decide again. The exception handler maps it to 409 Conflict. The coffee app of SE@UHD has the same
 * exception.
 *
 * @param message the message of the error response
 */
class StaleDataException(
    message: String
) : DomainException(message)
