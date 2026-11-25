package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of a rating, in which a [user] scores a [bean] with a [value] from 1 to 5. The migration
 * `V9__create_coffee_ratings_table.sql` creates the table `coffee_ratings` with a CHECK constraint that rejects any
 * other score. The table has no unique constraint, because a user can rate the same bean for several coffees (FR-12).
 * The table does not refer to a coffee yet, which the implementation of FR-12 needs.
 *
 * The foreign key `user_id` has `ON DELETE CASCADE`, because a rating belongs to its user. The foreign key `bean_id`
 * has the default `NO ACTION`, and PostgreSQL therefore does not delete a bean with a rating. The index on `user_id`
 * and `created_at` is for the query of the ratings of a user, newest first. The index on `bean_id` is for the query of
 * the ratings of a bean. JPA loads the user and the bean together with the rating, because the read methods of
 * the data adapter map the entity outside a transaction (see `CrudDataServiceImpl`).
 */
@jakarta.persistence.Entity
@Table(name = "coffee_ratings")
class CoffeeRatingEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:ManyToOne
    @field:JoinColumn(name = "bean_id", nullable = false)
    var bean: CoffeeBeanEntity? = null

    @field:Column(name = "value")
    var value: Int? = null
}
