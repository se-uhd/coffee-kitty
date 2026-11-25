package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a correction, in which an [admin] sets the coffee count of a [user] from [countBefore] to
 * [countAfter] and explains the correction in a [note] (FR-8 in `requirements/FR-8-correct-coffee-count.md`). Instances
 * are immutable, and Coffee Kitty never changes a stored correction. `CoffeeConsumptionService.correctCount` stores a
 * correction together with the corrected coffee count. Before that, it checks the correction against the Bean
 * Validation constraints on [countAfter] and [note] with `DomainValidator`. A coffee count has no upper limit of its
 * own: FR-8 leaves the limit open, so the only limit is the largest value of an `Int`.
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a correction that is not stored yet.
 * [createdAt] is the time of the correction in UTC.
 */
data class CountCorrection(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User,
    val admin: User,
    val countBefore: Int,
    @field:Min(value = 0, message = "The coffee count must not be negative.")
    val countAfter: Int,
    @field:Pattern(regexp = TextPatterns.NOT_BLANK, message = "The note of a correction must not be blank.")
    @field:Size(max = MAX_NOTE_LENGTH, message = NOTE_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_NUL_CHARACTER, message = NOTE_MESSAGE)
    val note: String
) : DomainModel<UUID> {
    /** The limits of a new correction. */
    companion object {
        /**
         * The maximum length of a note, which the team assistant set (FR-8). The column `count_corrections.note` has
         * the same width. `@Size` counts UTF-16 code units, so a character outside the Basic Multilingual Plane counts
         * as two.
         */
        const val MAX_NOTE_LENGTH = 500

        /** The message of the error response for a note that is too long or contains the NUL character. */
        const val NOTE_MESSAGE = "A note must have at most $MAX_NOTE_LENGTH characters and no NUL character."
    }
}
