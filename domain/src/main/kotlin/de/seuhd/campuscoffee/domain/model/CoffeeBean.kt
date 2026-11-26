package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a bean, one named kind of coffee beans that the group buys and that users rate. A live bean has
 * [active] true and a null [mergedIntoId], and its [name] is unique among the live beans, ignoring case. An admin can
 * merge a bean into another bean, for example to remove a duplicate with a misspelled name. The merged bean stays as a
 * record with [active] false and the id of the remaining bean in [mergedIntoId], because its ratings and bean
 * purchases still refer to it. A live bean can have the name of a merged bean.
 */
data class CoffeeBean(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @field:Pattern(regexp = TextPatterns.NOT_BLANK, message = NAME_MESSAGE)
    @field:Size(max = MAX_NAME_LENGTH, message = NAME_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_NUL_CHARACTER, message = NAME_MESSAGE)
    val name: String,
    val active: Boolean = true,
    val mergedIntoId: UUID? = null
) : DomainModel<UUID> {
    /** The limit of a name and the message for a name that breaks a rule. */
    companion object {
        /**
         * The maximum length of a name, which the team assistant set (FR-6). The column `coffee_beans.name` has the
         * same width.
         */
        const val MAX_NAME_LENGTH = 200

        /** The message of the error response for an invalid name. */
        const val NAME_MESSAGE =
            "The name of a bean must not be blank and must have at most $MAX_NAME_LENGTH characters and no NUL " +
                "character."
    }
}
