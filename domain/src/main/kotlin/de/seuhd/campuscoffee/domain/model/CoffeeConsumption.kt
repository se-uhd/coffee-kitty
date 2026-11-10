package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Immutable consumption domain model: the record that holds the coffee [count] of one [user]. Each user has exactly
 * one consumption, created with a [count] of zero together with the user. Adding a coffee stores a copy with the
 * raised [count].
 */
data class CoffeeConsumption(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val count: Int
) : DomainModel<UUID>
