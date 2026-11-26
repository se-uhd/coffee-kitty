package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of an expense, money that a [buyer] spent for the group. [expenseType] holds [BEANS_TYPE] for a bean
 * purchase with a [bean] and a [weightGrams], or [OTHER_TYPE] for any other expense, which has neither. A domain enum
 * with constants of these names needs no mapping code, because MapStruct converts an enum by the name of the constant.
 * The CHECK constraint `ck_expenses_split` requires that [privateAmountCents] and [kittyAmountCents] add up to
 * [amountCents]. The foreign keys `buyer_user_id` and `bean_id` have `NO ACTION`, because an expense moved money and
 * must not disappear with its buyer or its bean.
 */
@jakarta.persistence.Entity
@Table(name = "expenses")
class ExpenseEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "buyer_user_id", nullable = false)
    var buyer: UserEntity? = null

    @field:Column(name = "expense_type")
    var expenseType: String? = null

    @field:ManyToOne
    @field:JoinColumn(name = "bean_id")
    var bean: CoffeeBeanEntity? = null

    @field:Column(name = "weight_grams")
    var weightGrams: Int? = null

    @field:Column(name = "amount_cents")
    var amountCents: Int? = null

    @field:Column(name = "private_amount_cents")
    var privateAmountCents: Int? = null

    @field:Column(name = "kitty_amount_cents")
    var kittyAmountCents: Int? = null

    @field:Column(name = "note")
    var note: String? = null

    companion object {
        /** The value of [expenseType] for a bean purchase. */
        const val BEANS_TYPE = "BEANS"

        /** The value of [expenseType] for any other expense. */
        const val OTHER_TYPE = "OTHER"
    }
}
