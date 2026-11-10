package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA entity of a consumption, which holds a [user] and the coffee [count] of that user. The unique constraint
 * [USER_UNIQUE_CONSTRAINT] allows one row per user.
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
        /** Name of the unique constraint on `user_id`. */
        const val USER_UNIQUE_CONSTRAINT = "uq_coffee_consumptions_user"
    }
}
