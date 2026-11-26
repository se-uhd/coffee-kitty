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
 * The JPA entity of a consumption. [coffeeTimes] holds the time in UTC of each recent coffee, oldest first, in the
 * table `coffee_times`, which JPA loads eagerly (ADR-004). The column `coffee_times.price_cents` is for the amount by
 * which a coffee lowered the balance, so that an undo can raise it by the same amount (FR-11). It allows null, because
 * the coffees from before FR-5 have no price. The entity does not map the column yet, so every stored coffee time has a
 * null price, and the implementation of FR-5 has to map it.
 */
@jakarta.persistence.Entity
@Table(name = "coffee_consumptions")
class CoffeeConsumptionEntity : Entity() {
    // ADR-020 ON DELETE CASCADE, because a consumption belongs to its user
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:Column(name = "count")
    var count: Int? = null

    // ADR-020 ON DELETE CASCADE, because a coffee time belongs to its consumption
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
