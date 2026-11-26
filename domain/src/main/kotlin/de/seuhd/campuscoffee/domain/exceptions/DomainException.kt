package de.seuhd.campuscoffee.domain.exceptions

/**
 * The base class of the domain exceptions. The class is sealed, and `GlobalExceptionHandler` does not compile until it
 * maps a new subclass to a status.
 *
 * @property message the message of the error response
 * @property logMessage the message for the server log, without a value that identifies a user (ADR-015)
 */
sealed class DomainException(
    override val message: String,
    val logMessage: String = message
) : RuntimeException(message)
