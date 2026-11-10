package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a request breaks a rule of the domain or of a controller, such as a delta other than `+1`, a name with a
 * control character, or an id in the body of `POST /api/users`. The exception handler maps it to the status 400 Bad
 * Request with the error code `ValidationException`. Bean Validation checks the annotations on a DTO, such as
 * `@NotBlank` on a name, before the request reaches the controller. A request body that fails Bean Validation gets a
 * 400 with the error code `BadRequest` instead.
 *
 * @param message the message of the error response
 */
class ValidationException(
    message: String
) : RuntimeException(message)
