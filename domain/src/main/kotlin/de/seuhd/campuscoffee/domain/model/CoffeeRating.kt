package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a rating, in which a [user] scores a [bean] with a [value] from [MIN_VALUE] to [MAX_VALUE]. A
 * user rates the beans of a coffee, not the cup itself. Each coffee has at most one rating: Until the user adds the
 * next coffee, a new rating replaces the rating of the most recent coffee (FR-12). The ratings of different coffees
 * stay, also for the same bean. A rating moves no money. The Bean Validation constraints on [value] state the range of
 * a score, and a domain service checks them with `DomainValidator` before it stores a rating.
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
    @field:Min(value = MIN_VALUE.toLong(), message = VALUE_MESSAGE)
    @field:Max(value = MAX_VALUE.toLong(), message = VALUE_MESSAGE)
    val value: Int
) : DomainModel<UUID> {
    /** The range of a score and the message for a score outside it. */
    companion object {
        /** The lowest score of a rating. The team assistant set the scores from 1 to 5 (FR-12). */
        const val MIN_VALUE = 1

        /** The highest score of a rating. */
        const val MAX_VALUE = 5

        /** The message of the error response for a score outside the range. */
        const val VALUE_MESSAGE = "A rating must be from $MIN_VALUE to $MAX_VALUE."
    }
}
