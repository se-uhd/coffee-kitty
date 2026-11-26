package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of a payment, which is a deposit of a [user] or, without a [user], a kitty adjustment. The CHECK
 * constraints of `V7__create_payments_table.sql` reject an amount of zero, a deposit with an amount that is not
 * positive, and an empty note. JPA loads the user eagerly (ADR-004).
 */
@jakarta.persistence.Entity
@Table(name = "payments")
class PaymentEntity : Entity() {
    // ADR-020 NO ACTION, because a deposit is money in the kitty and must not disappear with its user
    @field:ManyToOne
    @field:JoinColumn(name = "user_id")
    var user: UserEntity? = null

    @field:Column(name = "amount_cents")
    var amountCents: Int? = null

    @field:Column(name = "note")
    var note: String? = null
}
