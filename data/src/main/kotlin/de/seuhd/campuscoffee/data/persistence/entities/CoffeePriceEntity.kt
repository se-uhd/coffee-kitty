package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Table

/**
 * JPA entity of the price per cup in euro cents ([amountCents]). A CHECK constraint rejects a negative price. The
 * migration `V6__create_coffee_prices_table.sql` creates the table `coffee_prices`, which holds at most one row. The
 * column `is_singleton` must be true, and the unique constraint [SINGLETON_UNIQUE_CONSTRAINT] on it allows only one row
 * with that value. The entity does not map the column, because the database sets its default. A new price updates the
 * stored row.
 */
@jakarta.persistence.Entity
@Table(name = "coffee_prices")
class CoffeePriceEntity : Entity() {
    @field:Column(name = "amount_cents")
    var amountCents: Int? = null

    companion object {
        /** The name of the unique constraint on the column `is_singleton`, which allows only one row. */
        const val SINGLETON_UNIQUE_CONSTRAINT = "uq_coffee_prices_singleton"
    }
}
