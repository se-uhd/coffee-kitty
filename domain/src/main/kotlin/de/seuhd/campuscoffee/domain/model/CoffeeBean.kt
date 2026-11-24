package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a bean, one named kind of coffee beans that the group buys and that users rate. Each bean
 * purchase and each rating refers to a bean.
 *
 * A live bean has [active] true and a null [mergedIntoId], and its [name] is unique among the live beans, ignoring
 * case. An admin can merge a bean into another bean, for example to remove a duplicate with a misspelled name. The
 * merged bean stays as a record, because its ratings and bean purchases still refer to it. Its [active] is false, and
 * its [mergedIntoId] is the id of the remaining bean. A live bean can have the name of a merged bean.
 *
 * The [name] is not empty and has at most [MAX_NAME_LENGTH] characters. The data module assigns [id], [createdAt], and
 * [updatedAt]. They are null on a bean that is not stored yet.
 */
data class CoffeeBean(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val name: String,
    val active: Boolean = true,
    val mergedIntoId: UUID? = null
) : DomainModel<UUID> {
    /** The limits that the table `coffee_beans` sets. */
    companion object {
        /** The maximum length of a name. The column `coffee_beans.name` has the same width. */
        const val MAX_NAME_LENGTH = 200
    }
}
