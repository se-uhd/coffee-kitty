package de.seuhd.campuscoffee.domain.implementation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

/** Unit test for [ConsumptionProperties], which refuses a negative grace period. */
class ConsumptionPropertiesTest {
    @Test
    fun `ConsumptionProperties throws IllegalArgumentException for a grace period of minus one nanosecond`() {
        assertThrows<IllegalArgumentException> { ConsumptionProperties(Duration.ofNanos(-1)) }
    }
}
