package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * JPA entity of a consumption, which holds a [user] and the coffee [count] of that user. The unique constraint
 * [USER_UNIQUE_CONSTRAINT] allows one row per user. [coffeeTimes] holds the time in UTC of each recent coffee that the
 * user did not undo and that no correction removed, oldest first. The migration `V3__create_coffee_times_table.sql`
 * creates the table `coffee_times` for the times, and its column `position` stores the order. JPA loads the times
 * together with the consumption, because the read methods of the data adapter map the entity outside a transaction (see
 * `CrudDataServiceImpl`).
 * The migration `V12__add_price_to_coffee_times.sql` adds the column `price_cents` to `coffee_times`. The column is for
 * the amount in euro cents by which a coffee lowered the balance. An undo has to raise the balance by the same amount
 * (FR-11 in `requirements/FR-11-credit-undone-coffee.md`). A CHECK constraint rejects a negative price. The column
 * allows null, because the coffees from before FR-5 have no price. The entity does not map the column yet, so every
 * stored coffee time has a null price. The implementation of FR-5 has to map it, for example with an embeddable class
 * that holds the time and the price of a coffee.
 */
@jakarta.persistence.Entity
@Table(name = "coffee_consumptions")
class CoffeeConsumptionEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:Column(name = "count")
    var count: Int? = null

    @field:ElementCollection(fetch = FetchType.EAGER)
    @field:CollectionTable(name = "coffee_times", joinColumns = [JoinColumn(name = "consumption_id")])
    @field:OrderColumn(name = "position")
    @field:Column(name = "added_at")
    var coffeeTimes: MutableList<LocalDateTime> = mutableListOf()

    companion object {
        /** The name of the unique constraint on `user_id`. */
        const val USER_UNIQUE_CONSTRAINT = "uq_coffee_consumptions_user"
    }
}
