package de.seuhd.campuscoffee.domain.implementation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Unit tests for [GracePeriod] with a fixed clock at 2026-01-15T12:00Z and a grace period of 5 minutes. The grace
 * period includes the instant at which it ends. The coffee app of SE@UHD has the same two tests.
 */
class GracePeriodTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC)
    private val gracePeriod = GracePeriod(ConsumptionProperties(Duration.ofMinutes(5)), clock)

    @Test
    fun `isOpen is true for a coffee recorded exactly one grace period ago`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T11:55:00"))).isTrue()
    }

    @Test
    fun `isOpen is false for a coffee recorded just over one grace period ago`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T11:54:59.999999999"))).isFalse()
    }
}
