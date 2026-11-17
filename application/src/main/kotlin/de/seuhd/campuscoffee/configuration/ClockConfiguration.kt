package de.seuhd.campuscoffee.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Provides the [Clock] from which the domain services read the current time. The acceptance tests replace it with a
 * clock that the step definitions set to the times in the scenarios.
 */
@Configuration
class ClockConfiguration {
    /** Returns the system clock in UTC. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
