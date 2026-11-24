package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA entity of the kitty balance, the cash in the kitty in euro cents ([balanceCents]). Deposits raise it, the kitty
 * shares of expenses lower it, and kitty adjustments change it in either direction. The migration
 * `V11__create_balance_tables.sql` creates the table `kitty_balance`. Its CHECK constraints allow only the row with the
 * id [SINGLETON_ID] and reject a negative balance. Coffee Kitty has no domain model, port, or adapter for the kitty
 * balance yet. No code writes the table, and the table has no row. The code that first stores a kitty balance has to
 * insert the row. [KittyBalanceRepository][de.seuhd.campuscoffee.data.persistence.repositories.KittyBalanceRepository]
 * already reads the row with a lock.
 *
 * The class does not extend [Entity], because the table has neither a random id nor timestamps (see
 * [UserBalanceEntity]).
 */
@jakarta.persistence.Entity
@Table(name = "kitty_balance")
class KittyBalanceEntity {
    @field:Id
    @field:Column(name = "id")
    var id: Int = SINGLETON_ID

    @field:Column(name = "balance_cents")
    var balanceCents: Long = 0

    companion object {
        /** The id of the only row, which the CHECK constraint of the column `id` requires. */
        const val SINGLETON_ID = 1
    }
}
