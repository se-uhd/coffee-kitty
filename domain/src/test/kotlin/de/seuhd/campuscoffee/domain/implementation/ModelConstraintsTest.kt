package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The unit tests for the Bean Validation constraints on the domain models that no domain service stores yet: the
 * payment, the bean, the rating, and the price per cup. Each test checks a model with [DomainValidator], as a domain
 * service does before it stores the model.
 */
class ModelConstraintsTest {
    private val domainValidator = domainValidator()

    private val bean = CoffeeBean(name = "Ethiopia Sidamo")

    @ParameterizedTest
    @ValueSource(ints = [1, 500])
    fun `requireValid accepts a payment with a note of 1 or 500 characters`(length: Int) {
        assertDoesNotThrow { domainValidator.requireValid(Payment(amountCents = 1000, note = "x".repeat(length))) }
    }

    @Test
    fun `requireValid accepts a payment without a note`() {
        assertDoesNotThrow { domainValidator.requireValid(Payment(amountCents = 1000)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "Cash\u0000"])
    fun `requireValid throws ValidationException for a payment with a blank note or a NUL character`(note: String) {
        assertThrows<ValidationException> { domainValidator.requireValid(Payment(amountCents = 1000, note = note)) }
    }

    @Test
    fun `requireValid throws ValidationException for a payment with a note of 501 characters`() {
        val note = "x".repeat(501)

        assertThrows<ValidationException> { domainValidator.requireValid(Payment(amountCents = 1000, note = note)) }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 200])
    fun `requireValid accepts a bean with a name of 1 or 200 characters`(length: Int) {
        assertDoesNotThrow { domainValidator.requireValid(CoffeeBean(name = "x".repeat(length))) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "Sidamo\u0000"])
    fun `requireValid throws ValidationException for a bean with a blank name or a NUL character`(name: String) {
        assertThrows<ValidationException> { domainValidator.requireValid(CoffeeBean(name = name)) }
    }

    @Test
    fun `requireValid throws ValidationException for a bean with a name of 201 characters`() {
        val name = "x".repeat(201)

        assertThrows<ValidationException> { domainValidator.requireValid(CoffeeBean(name = name)) }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 5])
    fun `requireValid accepts a rating with a score of 1 or 5`(value: Int) {
        val rating = CoffeeRating(user = TestFixtures.regularUser(), bean = bean, value = value)

        assertDoesNotThrow { domainValidator.requireValid(rating) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 6])
    fun `requireValid throws ValidationException for a rating with a score of 0 or 6`(value: Int) {
        val rating = CoffeeRating(user = TestFixtures.regularUser(), bean = bean, value = value)

        assertThrows<ValidationException> { domainValidator.requireValid(rating) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 100_000])
    fun `requireValid accepts a price per cup of 0 or 100,000 cents`(cents: Int) {
        assertDoesNotThrow { domainValidator.requireValid(CoffeePrice(amountCents = cents)) }
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 100_001])
    fun `requireValid throws ValidationException for a price per cup of -1 or 100,001 cents`(cents: Int) {
        assertThrows<ValidationException> { domainValidator.requireValid(CoffeePrice(amountCents = cents)) }
    }
}
