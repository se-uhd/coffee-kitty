package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Table
import java.util.UUID

/**
 * The JPA entity of a bean. The CHECK constraints of `V8__create_coffee_beans_table.sql` require that [active] is true
 * exactly when [mergedIntoId] is null and reject a bean merged into itself. The unique index [NAME_UNIQUE_CONSTRAINT]
 * on `lower(name)` covers only the live beans, so a live bean can have the name of a merged bean.
 */
@jakarta.persistence.Entity
@Table(name = "coffee_beans")
class CoffeeBeanEntity : Entity() {
    @field:Column(name = "name")
    var name: String? = null

    @field:Column(name = "active")
    var active: Boolean? = null

    // ADR-020 NO ACTION keeps a bean that a merged bean refers to, ADR-021 an id avoids loading the chain
    @field:Column(name = "merged_into_id")
    var mergedIntoId: UUID? = null

    companion object {
        /** The name of the partial unique index on `lower(name)` for the live beans. */
        const val NAME_UNIQUE_CONSTRAINT = "uq_coffee_beans_active_name"
    }
}
