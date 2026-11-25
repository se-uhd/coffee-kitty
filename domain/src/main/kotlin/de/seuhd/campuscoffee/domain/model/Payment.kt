package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a payment, an amount of money in euro cents ([amountCents]) that an admin records for the kitty.
 * The nullable [user] distinguishes the two kinds of payment:
 * - With a [user], the payment is a deposit, cash that the user paid into the kitty. A deposit raises the balance of
 *   the user and the kitty by its amount, and the table `payments` accepts only a positive amount.
 * - Without a [user], the payment is a kitty adjustment, which changes only the kitty, for example by the initial cash
 *   in the kitty. Its amount can be negative but not zero.
 *
 * The optional [note] is null or a text with 1 to [MAX_NOTE_LENGTH] characters, which the Bean Validation constraints
 * on [note] state. A domain service checks them with `DomainValidator` before it stores a payment. The limits of
 * [amountCents] differ between the two kinds of payment, so the domain service of each kind has to check them. The data
 * module assigns [id], [createdAt], and [updatedAt]. They are null on a payment that is not stored yet. [createdAt] is
 * the time of the payment in UTC.
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
