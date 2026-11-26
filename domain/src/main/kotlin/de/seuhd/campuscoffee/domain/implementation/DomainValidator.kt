package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import jakarta.validation.Validator
import org.springframework.stereotype.Component

/**
 * Checks domain objects against the Bean Validation constraints on their properties, such as `@Size` on
 * `User.loginName` (ADR-009). A violated constraint becomes a [ValidationException] with the message of the constraint.
 * The exception contains each message once and in alphabetical order, so that its message does not depend on the order
 * in which the validator checks the constraints.
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
