package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of a rating, in which a [user] scores a [bean] with a [value] from 1 to 5. The table has no unique
 * constraint, because a user can rate the same bean for several coffees (FR-12), and it does not refer to a coffee yet,
 * which the implementation of FR-12 needs. JPA loads the user and the bean eagerly (ADR-004).
 */
@jakarta.persistence.Entity
@Table(name = "coffee_ratings")
class CoffeeRatingEntity : Entity() {
    // ADR-020 ON DELETE CASCADE, because a rating belongs to its user
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    // ADR-020 NO ACTION, so PostgreSQL keeps a bean with a rating
    @field:ManyToOne
    @field:JoinColumn(name = "bean_id", nullable = false)
    var bean: CoffeeBeanEntity? = null

    @field:Column(name = "value")
    var value: Int? = null
}
