package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a payment, an amount of money in euro cents that an admin records for the kitty. With a [user],
 * the payment is a deposit, which raises the balance of the user and the kitty by its positive amount. Without a
 * [user], it is a kitty adjustment, which changes only the kitty, such as the initial cash in the kitty, and its amount
 * can be negative but not zero. The limits of [amountCents] differ between the two kinds, so the domain service of each
 * kind has to check them. [createdAt] is the time of the payment in UTC.
 */
data class Payment(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User? = null,
    val amountCents: Int,
    @field:Pattern(regexp = TextPatterns.NOT_BLANK, message = "The note of a payment must not be blank.")
    @field:Size(max = MAX_NOTE_LENGTH, message = NOTE_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_NUL_CHARACTER, message = NOTE_MESSAGE)
    val note: String? = null
) : DomainModel<UUID> {
    /** The limit of a note and the message for a note that breaks it. */
    companion object {
        /**
         * The maximum length of a note, which the team assistant set (FR-4 and FR-8). The column `payments.note` has
         * the same width. `@Size` counts UTF-16 code units, so a character outside the Basic Multilingual Plane counts
         * as two.
         */
        const val MAX_NOTE_LENGTH = 500

        /** The message of the error response for a note that is too long or contains the NUL character. */
        const val NOTE_MESSAGE = "A note must have at most $MAX_NOTE_LENGTH characters and no NUL character."
    }
}
