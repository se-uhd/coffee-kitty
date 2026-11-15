package de.seuhd.campuscoffee.domain.implementation

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The grace period after a coffee, in which the user can still undo the coffee. [CoffeeConsumptionServiceImpl.undo]
 * calls [isOpen] for an undo. The current time comes from the application [Clock], which also provides the time of each
 * coffee. The coffee app of SE@UHD has a class with the same name and the same [isOpen].
 */
@Component
class GracePeriod(
    private val consumptionProperties: ConsumptionProperties,
    private val clock: Clock
) {
    /**
     * Returns whether a coffee added at [recordedAt] can still be undone. The result is true until the grace period
     * has passed, including the instant at which it ends. For a coffee added at 10:00:00 and a grace period of 5
     * minutes, an undo at 10:05:00 is accepted, and an undo at 10:05:01 is refused.
     *
     * @param recordedAt the time at which the user added the coffee, in UTC
     */
    fun isOpen(recordedAt: LocalDateTime): Boolean {
        val now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
        return !recordedAt.isBefore(now.minus(consumptionProperties.gracePeriod))
    }
}
