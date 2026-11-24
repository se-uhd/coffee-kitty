package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model of a consumption, which holds the coffee [count] of one [user]. Each user has exactly one consumption,
 * which `UserService.create` or `UserInternalService.create` creates together with the user at a count of zero.
 * Instances are immutable. To add a coffee, the domain stores a copy with the raised [count].
 * [coffeeTimes] holds the time in UTC of each recent coffee that the user added and did not undo, oldest first. When
 * the user adds a coffee, the domain drops the times of the coffees outside their grace period. The user can no longer
 * undo these coffees. An undo compares the time of the most recent coffee with the current time and stores the lowered
 * count without that time. The coffee before then becomes the most recent coffee. As in the coffee app of SE@UHD, the
 * user can undo it within its own grace period. Coffees from before Coffee Kitty 0.2.0 and coffees that a correction
 * adds have no time, and the user cannot undo them. A correction that lowers the count removes the times of the newest
 * coffees.
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int,
    val coffeeTimes: List<LocalDateTime> = emptyList()
) : DomainModel<UUID>
