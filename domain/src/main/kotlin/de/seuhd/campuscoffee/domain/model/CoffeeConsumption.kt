package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Immutable coffee consumption domain model: a single user's running coffee count, referencing its [user] and
 * holding the running [count]. There is exactly one consumption per user, created at zero together with the user.
 * Adding a coffee stores a copy with the raised [count].
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int
) : DomainModel<UUID>
