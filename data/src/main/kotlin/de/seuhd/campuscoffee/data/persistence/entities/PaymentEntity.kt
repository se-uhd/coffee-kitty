package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA entity of a payment, which is a deposit of a [user] or, without a [user], a kitty adjustment. [amountCents] is
 * the amount in euro cents, and [note] is an optional text. The migration `V7__create_payments_table.sql` creates the
 * table `payments`. Its CHECK constraints reject an amount of zero, a deposit with an amount that is not positive, and
 * an empty note. The column `note` has the width `Payment.MAX_NOTE_LENGTH`.
 *
 * The foreign key `user_id` has the default `NO ACTION`, because a deposit is money in the kitty and must not disappear
 * with its user. Deleting a user with a deposit fails. The index on `user_id` and `created_at` is for the query of the
 * deposits of a user, newest first. JPA loads the user together with the payment, because the read methods of
 * the data adapter map the entity outside a transaction (see `CrudDataServiceImpl`).
 */
@jakarta.persistence.Entity
@Table(name = "payments")
class PaymentEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id")
    var user: UserEntity? = null

    @field:Column(name = "amount_cents")
    var amountCents: Int? = null

    @field:Column(name = "note")
    var note: String? = null
}
