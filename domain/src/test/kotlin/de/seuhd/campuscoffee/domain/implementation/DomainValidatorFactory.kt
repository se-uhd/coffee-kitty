package de.seuhd.campuscoffee.domain.implementation

import jakarta.validation.Validation

/**
 * Returns a [DomainValidator] with the default validator of Jakarta Bean Validation, for a unit test that creates a
 * domain service without a Spring context. The service then checks the constraints on the domain models as in the app.
 */
fun domainValidator(): DomainValidator = DomainValidator(Validation.buildDefaultValidatorFactory().validator)
