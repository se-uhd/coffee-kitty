package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a correction, in which an [admin] sets the coffee count of a [user] from [countBefore] to
 * [countAfter] and explains the correction in a [note] (FR-8 in `requirements/FR-8-correct-coffee-count.md`). Instances
 * are immutable, and Coffee Kitty never changes a stored correction. `CoffeeConsumptionService.correctCount` stores a
 * correction together with the corrected coffee count. Before that, it checks the correction against the Bean
 * Validation constraints on [countAfter] and [note] with `DomainValidator`.
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
    @field:Max(value = MAX_TOTAL.toLong(), message = "The coffee count must not exceed $MAX_TOTAL.")
    val countAfter: Int,
    @field:NotBlank(message = "The note of a correction must not be blank.")
    @field:Size(max = MAX_NOTE_LENGTH, message = NOTE_MESSAGE)
    @field:Pattern(regexp = NO_NUL_CHARACTER_PATTERN, message = NOTE_MESSAGE)
    val note: String
) : DomainModel<UUID> {
    /** The limits of a new correction. */
    companion object {
        /** The maximum length of a note. The column `count_corrections.note` has the same width. */
        const val MAX_NOTE_LENGTH = 500

        /**
         * The upper limit of a corrected coffee count, as in the coffee app of SE@UHD. A real coffee count stays far
         * below the limit. Without a limit, the coffee count could overflow when the user adds a coffee. The limit is a
         * TBD in FR-8.
         */
        const val MAX_TOTAL = 1_000_000

        /** The regular expression that a whole note must match: no NUL character, which PostgreSQL cannot store. */
        const val NO_NUL_CHARACTER_PATTERN = "[^\\u0000]*"

        /** The message of the error response for a note that is too long or contains the NUL character. */
        const val NOTE_MESSAGE = "A note must have at most $MAX_NOTE_LENGTH characters and no NUL character."
    }
}
