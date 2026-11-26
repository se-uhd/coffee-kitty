package de.seuhd.campuscoffee.domain.implementation

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * The configuration of the grace period, in which a user can undo a coffee. The class is in the domain module, because
 * the domain checks the grace period and cannot depend on the api module or the application module. With this class,
 * the IDE can resolve the key in `application.yaml`. The coffee app of SE@UHD has the same class.
 *
 * @property gracePeriod the time after a coffee in which a user can undo it (`campus-coffee.consumption.grace-period`)
 */
@ConfigurationProperties("campus-coffee.consumption")
data class ConsumptionProperties(
    val gracePeriod: Duration = Duration.ofMinutes(DEFAULT_GRACE_MINUTES)
) {
    init {
        require(!gracePeriod.isNegative) {
            "campus-coffee.consumption.grace-period must not be negative, but is $gracePeriod."
        }
    }

    private companion object {
        private const val DEFAULT_GRACE_MINUTES = 5L
    }
}
