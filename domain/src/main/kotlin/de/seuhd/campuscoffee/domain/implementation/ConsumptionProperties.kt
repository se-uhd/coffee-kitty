package de.seuhd.campuscoffee.domain.implementation

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Domain configuration of the coffee consumption rules. The class is in the domain module, because the domain
 * enforces the rule and cannot depend on the api or application modules. The typed binding lets the IDE resolve the
 * key in `application.yaml`. The coffee app of SE@UHD has the same class.
 *
 * @property gracePeriod how long after adding a coffee a user can still undo it with
 *   `POST /api/users/{userId}/consumption/undo`. A later undo gets the status 409 Conflict. Spring binds the value
 *   from `campus-coffee.consumption.grace-period`, and the default is 5 minutes, as in `application.yaml`. The
 *   value must not be negative.
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
