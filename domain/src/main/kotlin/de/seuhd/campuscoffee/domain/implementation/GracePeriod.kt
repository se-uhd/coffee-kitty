package de.seuhd.campuscoffee.domain.implementation

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The grace period after a coffee, in which the user can still undo the coffee. [CoffeeConsumptionServiceImpl.undo]
 * calls [isOpen] for an undo, and [CoffeeConsumptionServiceImpl.undoableUntil] calls [isOpen] and [closesAt] for the
 * undo status. The current time comes from the application [Clock], which also provides the time of each coffee. The
 * coffee app of SE@UHD has a class with the same name and the same [isOpen] and [closesAt].
 */
@Component
class GracePeriod(
    private val consumptionProperties: ConsumptionProperties,
    private val clock: Clock
) {
    /**
     * Returns whether a user can still undo a coffee added at [recordedAt]. The result is true until the grace period
     * ends, including that instant. With a grace period of 5 minutes, an undo of a coffee from 10:00:00 succeeds at
     * 10:05:00 and fails at 10:05:01.
     *
     * @param recordedAt the time at which the user added the coffee, in UTC
     */
    fun isOpen(recordedAt: LocalDateTime): Boolean {
        val now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
        return !recordedAt.isBefore(now.minus(consumptionProperties.gracePeriod))
    }

    /**
     * Returns the end of the grace period of a coffee added at [recordedAt], the last instant at which [isOpen] returns
     * true.
     *
     * @param recordedAt the time at which the user added the coffee, in UTC
     */
    fun closesAt(recordedAt: LocalDateTime): LocalDateTime = recordedAt.plus(consumptionProperties.gracePeriod)
}
