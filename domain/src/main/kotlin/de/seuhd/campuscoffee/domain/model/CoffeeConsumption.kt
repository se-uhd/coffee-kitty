package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model of a consumption, which holds the coffee [count] of one [user]. Each user has exactly one consumption,
 * which `UserService.create` creates together with the user at a count of zero. Instances are immutable. Adding a
 * coffee stores a copy with the raised [count].
 *
 * [coffeeTimes] holds the time in UTC of each recent coffee that the user added and did not undo, oldest first. Adding
 * a coffee drops the times whose grace period has passed, because no undo can reach them. An undo compares the last
 * time with the current time and stores the lowered count without that time. The coffee before then becomes the most
 * recent coffee, which the user can undo while it is within its own grace period, as in the coffee app of SE@UHD.
 * Coffees added before Coffee Kitty 0.2.0 have no time and cannot be undone.
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int,
    val coffeeTimes: List<LocalDateTime> = emptyList()
) : DomainModel<UUID>
