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
 * period includes the instant at which it ends. The boundary at 11:55:00 splits the times of the coffees into two
 * partitions: [GracePeriod.isOpen] is true for a coffee from 11:55:00 on and false for an earlier coffee. The tests
 * take one time inside each partition (12:00:00 and 11:00:00) and the two times at the boundary. The coffee app of
 * SE@UHD has the same two tests at the boundary. The last test checks that [GracePeriod] reads the grace period from
 * [ConsumptionProperties].
 */
class GracePeriodTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC)
    private val gracePeriod = GracePeriod(ConsumptionProperties(Duration.ofMinutes(5)), clock)

    @Test
    fun `isOpen returns true for a coffee added at the current time`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T12:00:00"))).isTrue()
    }

    @Test
    fun `isOpen returns true for a coffee added exactly one grace period ago`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T11:55:00"))).isTrue()
    }

    @Test
    fun `isOpen returns false for a coffee added one grace period and one nanosecond ago`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T11:54:59.999999999"))).isFalse()
    }

    @Test
    fun `isOpen returns false for a coffee added one hour ago`() {
        assertThat(gracePeriod.isOpen(LocalDateTime.parse("2026-01-15T11:00:00"))).isFalse()
    }

    @Test
    fun `isOpen returns true for a coffee added 6 minutes ago with a grace period of 10 minutes`() {
        val tenMinutes = GracePeriod(ConsumptionProperties(Duration.ofMinutes(10)), clock)

        assertThat(tenMinutes.isOpen(LocalDateTime.parse("2026-01-15T11:54:00"))).isTrue()
    }
}
