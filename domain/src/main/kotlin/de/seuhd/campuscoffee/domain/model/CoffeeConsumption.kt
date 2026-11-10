package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model of a consumption, which holds the coffee [count] of one [user]. Each user has exactly one consumption,
 * which `UserService.create` creates together with the user at a count of zero. Instances are immutable. Adding a
 * coffee stores a copy with the raised [count].
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int
) : DomainModel<UUID>
