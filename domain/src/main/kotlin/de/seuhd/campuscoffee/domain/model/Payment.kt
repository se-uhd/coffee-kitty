package de.seuhd.campuscoffee.domain.model

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
 * The optional [note] is null or a text with 1 to [MAX_NOTE_LENGTH] characters. The data module assigns [id],
 * [createdAt], and [updatedAt]. They are null on a payment that is not stored yet. [createdAt] is the time of the
 * payment in UTC.
 */
data class Payment(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val user: User? = null,
    val amountCents: Int,
    val note: String? = null
) : DomainModel<UUID> {
    /** The limits that the table `payments` sets. */
    companion object {
        /** The maximum length of a note. The column `payments.note` has the same width. */
        const val MAX_NOTE_LENGTH = 500
    }
}
