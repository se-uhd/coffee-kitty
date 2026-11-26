package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * The JPA entity of the kitty balance in euro cents. The CHECK constraints of `V11__create_balance_tables.sql` allow
 * only the row with the id [SINGLETON_ID] and reject a negative balance. No code writes the table yet, and the table
 * has no row, so the code that first stores a kitty balance has to insert the row. The class does not extend [Entity],
 * because the table has neither a random id nor timestamps.
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
