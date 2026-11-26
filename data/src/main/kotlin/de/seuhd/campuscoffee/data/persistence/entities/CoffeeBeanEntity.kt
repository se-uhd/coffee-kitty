package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Table
import java.util.UUID

/**
 * The JPA entity of a bean. The CHECK constraints of `V8__create_coffee_beans_table.sql` require that [active] is true
 * exactly when [mergedIntoId] is null and reject a bean merged into itself. The unique index [NAME_UNIQUE_CONSTRAINT]
 * on `lower(name)` covers only the live beans, so a live bean can have the name of a merged bean. [mergedIntoId] is an
 * id and not a JPA association, because JPA would load a chain of merged beans through an association. The foreign key
 * `merged_into_id` has `NO ACTION`, so PostgreSQL does not delete a bean while a merged bean refers to it.
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
