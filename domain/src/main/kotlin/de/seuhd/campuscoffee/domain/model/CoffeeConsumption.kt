package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a consumption, which holds the coffee [count] of one [user]. Each user has exactly one
 * consumption, which the domain creates together with the user at a count of zero. [coffeeTimes] holds the time in UTC
 * of each recent coffee that the user added and did not undo, oldest first. After an undo, the user can undo the
 * coffee before within its own grace period, as in the coffee app of SE@UHD. Coffees from before Coffee Kitty 0.2.0 and
 * coffees that a correction adds have no time, and the user cannot undo them.
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int,
    val coffeeTimes: List<LocalDateTime> = emptyList()
) : DomainModel<UUID>
