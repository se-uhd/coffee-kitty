package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of a correction, in which an [admin] set the coffee count of a [user] from [countBefore] to
 * [countAfter] and explained the correction in a [note]. The foreign key `user_id` has `ON DELETE CASCADE`, because a
 * correction belongs to its user, as a consumption does. The foreign key `admin_id` has `NO ACTION`, because a
 * correction does not belong to its admin, so PostgreSQL does not delete an admin who corrected the coffee count of
 * another user. JPA loads both users eagerly (ADR-004).
 */
@jakarta.persistence.Entity
@Table(name = "count_corrections")
class CountCorrectionEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:ManyToOne
    @field:JoinColumn(name = "admin_id", nullable = false)
    var admin: UserEntity? = null

    @field:Column(name = "count_before")
    var countBefore: Int? = null

    @field:Column(name = "count_after")
    var countAfter: Int? = null

    @field:Column(name = "note")
    var note: String? = null
}
