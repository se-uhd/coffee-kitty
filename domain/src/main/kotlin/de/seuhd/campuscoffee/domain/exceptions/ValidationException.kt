package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a request breaks a rule of the domain or the controller (a 400), such as a `delta` other than `+1`, a
 * name with a control character, or an id in the body of a new user. Bean Validation in the api module checks the
 * constraints on the DTOs first, such as `@NotBlank` on a name, and rejects a violation with the error code
 * `BadRequest`.
 *
 * @param message the validation error message
 */
class ValidationException(
    message: String
) : RuntimeException(message)
