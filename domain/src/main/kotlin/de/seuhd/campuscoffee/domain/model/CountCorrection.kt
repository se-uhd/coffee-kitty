package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a correction, in which an [admin] sets the coffee count of a [user] from [countBefore] to
 * [countAfter] and explains the correction in a [note] (FR-8 in `requirements/FR-8-correct-coffee-count.md`). Instances
 * are immutable, and Coffee Kitty never changes a stored correction. `CoffeeConsumptionService.correctCount` stores a
 * correction together with the corrected coffee count.
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
    val countAfter: Int,
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
    }
}
