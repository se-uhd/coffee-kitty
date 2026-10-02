package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a request is malformed or violates a business rule (a 400), such as a `delta` other than `+1` or a
 * login name that breaks the rules in [de.seuhd.campuscoffee.domain.model.User]'s companion object. Carries a
 * human-readable message. The controller layer's Bean Validation reports the field-level constraints before a DTO
 * reaches the domain.
 *
 * @param message the validation error message
 */
class ValidationException(
    message: String
) : RuntimeException(message)
