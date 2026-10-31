package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA entity for a user's consumption: a single [user] reference and the running [count]. There is one row per user
 * (a named unique constraint on `user_id`).
 */
@jakarta.persistence.Entity
@Table(name = "coffee_consumptions")
class CoffeeConsumptionEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:Column(name = "count")
    var count: Int? = null

    companion object {
        /** Name of the unique constraint on `user_id`, which allows one consumption per user. */
        const val USER_UNIQUE_CONSTRAINT = "uq_coffee_consumptions_user"
    }
}
