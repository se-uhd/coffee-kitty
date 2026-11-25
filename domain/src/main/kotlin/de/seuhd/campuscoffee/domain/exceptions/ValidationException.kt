package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a value in a request breaks a rule of the domain. Examples are a delta other than `+1`, a login name of
 * 256 characters, and a name with a control character. The exception handler maps the exception to the status 400 Bad
 * Request with the error code `ValidationException`.
 *
 * The api module checks only the structure of a request body: Jackson rejects a missing field, a value of the wrong
 * JSON type, and an unknown field. Such a request gets a 400 with the error code
 * `BadRequest` before it reaches the domain. Every rule for a value is in the domain, so that it also holds for a user
 * that the bootstrap admin loader or a test creates without a request. A domain model declares such a rule as a Bean
 * Validation constraint, and `DomainValidator` throws this exception for a violated constraint.
 *
 * @param message the message of the error response
 */
class ValidationException(
    message: String
) : DomainException(message)
