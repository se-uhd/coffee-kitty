package de.seuhd.campuscoffee.tests.acceptance

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A [Clock] in UTC that the step definitions set to a time of day, such as 10:05:00 on [DATE]. A step of an acceptance
 * scenario can contain the time of a coffee, an undo, or a correction. With this clock in the Spring context, the
 * scenarios do not depend on the time at which they run. No scenario has to wait 5 minutes for the end of the grace
 * period.
 */
class SettableClock : Clock() {
    @Volatile
    private var instant: Instant = DATE.atStartOfDay().toInstant(ZoneOffset.UTC)

    fun setTime(time: LocalTime) {
        instant = DATE.atTime(time).toInstant(ZoneOffset.UTC)
    }

    override fun instant(): Instant = instant

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = fixed(instant, zone)

    private companion object {
        /** The date of every time in a scenario. */
        val DATE: LocalDate = LocalDate.of(2026, 11, 16)
    }
}
