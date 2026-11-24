package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Table
import java.util.UUID

/**
 * The JPA entity of a bean with a [name]. The bean is live while [mergedIntoId] is null. A merged bean has [active]
 * false, and its [mergedIntoId] is the id of the bean into which an admin merged it. The CHECK constraint
 * `ck_coffee_beans_active` requires that [active] is true exactly when [mergedIntoId] is null, and
 * `ck_coffee_beans_merged_into_other` rejects a bean merged into itself. The migration
 * `V8__create_coffee_beans_table.sql` creates the table `coffee_beans`. The column `name` has the width
 * `CoffeeBean.MAX_NAME_LENGTH`, and a CHECK constraint rejects an empty name.
 *
 * The unique index [NAME_UNIQUE_CONSTRAINT] on `lower(name)` covers only the rows with a null `merged_into_id`. Two
 * live beans therefore cannot have the same name, ignoring case, but a live bean can have the name of a merged bean.
 * [mergedIntoId] is a plain id column and not a JPA association, because JPA would load a chain of merged beans through
 * an association. The foreign key `merged_into_id` has the default `NO ACTION`. With this default, PostgreSQL does not
 * delete a bean while a merged bean refers to it.
 */
@jakarta.persistence.Entity
@Table(name = "coffee_beans")
class CoffeeBeanEntity : Entity() {
    @field:Column(name = "name")
    var name: String? = null

    @field:Column(name = "active")
    var active: Boolean? = null

    @field:Column(name = "merged_into_id")
    var mergedIntoId: UUID? = null

    companion object {
        /** The name of the partial unique index on `lower(name)` for the live beans. */
        const val NAME_UNIQUE_CONSTRAINT = "uq_coffee_beans_active_name"
    }
}
