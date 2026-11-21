package de.seuhd.campuscoffee.domain.implementation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

/**
 * Unit tests for [ConsumptionProperties], which accepts every grace period that is not negative. Two tests cover the
 * boundary: a grace period of zero is the smallest valid value, and a grace period of minus one nanosecond is the
 * largest invalid value.
 */
class ConsumptionPropertiesTest {
    @Test
    fun `ConsumptionProperties without a value has a grace period of 5 minutes`() {
        assertThat(ConsumptionProperties().gracePeriod).isEqualTo(Duration.ofMinutes(5))
    }

    @Test
    fun `ConsumptionProperties with a grace period of zero keeps the value`() {
        assertThat(ConsumptionProperties(Duration.ZERO).gracePeriod).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `ConsumptionProperties with a grace period of minus one nanosecond throws IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> { ConsumptionProperties(Duration.ofNanos(-1)) }

        assertThat(exception).hasMessageContaining("campus-coffee.consumption.grace-period must not be negative")
    }
}
