package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a value in a request breaks a rule of the domain. Examples are a delta other than `+1`, a login name of
 * 256 characters, and a name with a control character. The exception handler maps the exception to the status 400 Bad
 * Request with the error code `ValidationException`. A request body that Jackson cannot read gets a 400 with the error
 * code `BadRequest` instead (ADR-006, ADR-008, and ADR-009).
 *
 * @param message the message of the error response
 */
class ValidationException(
    message: String
) : DomainException(message)
