package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import jakarta.validation.Validator
import org.springframework.stereotype.Component

/**
 * Checks domain objects against the Bean Validation constraints on their properties, such as `@Size` on
 * `User.loginName`. A domain model declares each limit of a value as a constraint with a message, and a domain service
 * calls this class before it stores the object. The check therefore applies to every caller of the domain service,
 * including the bootstrap admin loader and the test setup, which send no request.
 *
 * A violated constraint becomes a [ValidationException] (400) with the message of the constraint. Several constraints
 * on one property can have the same message, which the exception then contains once. The messages of several
 * violations are in alphabetical order, so that the message of the exception does not depend on the order in which
 * the validator checks the constraints.
 *
 * @param validator the validator of Jakarta Bean Validation, which Spring Boot provides as a bean
 */
@Component
class DomainValidator(
    private val validator: Validator
) {
    /**
     * Checks [model] against the constraints on its properties.
     *
     * @param model the domain object to check
     * @throws ValidationException if a value of [model] violates a constraint
     */
    fun requireValid(model: Any) {
        val violations = validator.validate(model)
        if (violations.isNotEmpty()) {
            throw ValidationException(
                violations
                    .map { it.message }
                    .distinct()
                    .sorted()
                    .joinToString(" ")
            )
        }
    }
}
