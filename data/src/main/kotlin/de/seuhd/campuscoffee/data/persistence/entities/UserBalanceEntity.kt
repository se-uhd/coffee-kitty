package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * The JPA entity of the balance of one user in euro cents, negative when the user owes the kitty money. No code writes
 * the table yet. Coffee Kitty does not store the time and the price of every past coffee, so it cannot compute a
 * balance afterwards. A domain service has to change the balance in the same transaction as a coffee, an undo, a
 * correction, a deposit, or an expense. The class extends neither [Entity] nor [PersistableEntity] (ADR-021), so a data
 * adapter reads and writes the table through the repository, and `save()` of a new balance runs a SELECT first.
 */
@jakarta.persistence.Entity
@Table(name = "user_balance")
class UserBalanceEntity {
    // ADR-020 ON DELETE CASCADE, because the row holds the balance of one user
    @field:Id
    @field:Column(name = "user_id")
    var userId: UUID? = null

    @field:Column(name = "balance_cents")
    var balanceCents: Long = 0
}
