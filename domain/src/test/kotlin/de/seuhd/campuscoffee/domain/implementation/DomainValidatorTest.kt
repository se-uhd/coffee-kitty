package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * The unit tests for [DomainValidator] with the default validator of Jakarta Bean Validation. [Cup] is a class with
 * constraints that only these tests use, so that the tests do not depend on the limits of a domain model.
 */
class DomainValidatorTest {
    private data class Cup(
        @field:Size(max = 5, message = NAME_MESSAGE)
        @field:Pattern(regexp = "[a-z]*", message = NAME_MESSAGE)
        val name: String,
        @field:Max(value = 500, message = VOLUME_MESSAGE)
        val volume: Int
    )

    private val domainValidator = domainValidator()

    @Test
    fun `requireValid throws no exception for an object that meets every constraint`() {
        assertDoesNotThrow { domainValidator.requireValid(Cup(name = "mug", volume = 250)) }
    }

    @Test
    fun `requireValid throws ValidationException with the message of the violated constraint`() {
        val exception = assertThrows<ValidationException> { domainValidator.requireValid(Cup("mug", 501)) }

        assertThat(exception.message).isEqualTo(VOLUME_MESSAGE)
    }

    @Test
    fun `requireValid states the message of two violated constraints on one property once`() {
        val exception = assertThrows<ValidationException> { domainValidator.requireValid(Cup("ESPRESSO", 250)) }

        assertThat(exception.message).isEqualTo(NAME_MESSAGE)
    }

    @Test
    fun `requireValid states the messages of two properties in alphabetical order`() {
        val exception = assertThrows<ValidationException> { domainValidator.requireValid(Cup("ESPRESSO", 501)) }

        assertThat(exception.message).isEqualTo("$NAME_MESSAGE $VOLUME_MESSAGE")
    }

    private companion object {
        private const val NAME_MESSAGE = "A name has at most 5 lowercase letters."
        private const val VOLUME_MESSAGE = "The volume is at most 500 milliliters."
    }
}
