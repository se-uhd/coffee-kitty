package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model of a rating, in which a [user] scores a [bean] with a [value] from [MIN_VALUE] to [MAX_VALUE]. A user
 * rates the beans of a coffee, not the cup itself. The ratings of a user for the same bean accumulate over time,
 * because a new rating does not replace an earlier one. A rating moves no money.
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a rating that is not stored yet.
 * [createdAt] is the time of the rating in UTC.
 */
data class CoffeeRating(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val bean: CoffeeBean,
    val value: Int
) : DomainModel<UUID> {
    /** The limits that the table `coffee_ratings` sets. */
    companion object {
        /** The lowest score of a rating. */
        const val MIN_VALUE = 1

        /** The highest score of a rating. */
        const val MAX_VALUE = 5
    }
}
