package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The JPA entity of an expense, money that a [buyer] spent for the group. The migration
 * `V10__create_expenses_table.sql` creates the table `expenses`. Coffee Kitty has no domain model, data port, entity
 * mapper, or data adapter for expenses yet.
 * [ExpenseRepository][de.seuhd.campuscoffee.data.persistence.repositories.ExpenseRepository] already provides a query
 * for the expenses of a buyer.
 *
 * [expenseType] holds [BEANS_TYPE] for a bean purchase or [OTHER_TYPE] for any other expense, such as filters or milk.
 * A domain enum with constants of these names needs no mapping code, because MapStruct converts between an enum and a
 * `String` by the name of the constant. A bean purchase has a [bean] and a weight ([weightGrams]), and any other
 * expense has neither. The CHECK constraint `ck_expenses_type` enforces both cases.
 *
 * [amountCents] is the total in euro cents. The CHECK constraint `ck_expenses_split` requires that the private share
 * [privateAmountCents] and the kitty share [kittyAmountCents] add up to the total. The private share raises the balance
 * of the buyer, and the kitty share lowers the kitty. Further CHECK constraints reject a total or a weight that is not
 * positive, a negative share, and an empty [note]. The column `note` has a width of 500 characters.
 *
 * The foreign keys `buyer_user_id` and `bean_id` have the default `NO ACTION`, because an expense moved money and must
 * not disappear with its buyer or its bean. PostgreSQL therefore does not delete a buyer or a bean with an expense. JPA
 * loads the buyer and the bean together with the expense, because a data adapter maps the entity outside a transaction
 * (see `CrudDataServiceImpl`).
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
