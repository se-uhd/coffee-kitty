package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a person whose coffees the app counts. Instances are immutable. The constants in the companion
 * object are the limits for a new user. `UserServiceImpl.create` checks these limits for every new user, including the
 * user fixtures and the bootstrap admin. It also refuses a blank name and a name with a control character. The api
 * module checks only that a request contains every field. `CreateUserDto` repeats the limits in `@Schema` annotations
 * to document them in the OpenAPI definition.
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a user that is not stored yet. [role]
 * determines which actions the user may perform, and it is [Role.USER] by default.
 */
data class User(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val loginName: String,
    val emailAddress: String,
    val firstName: String,
    val lastName: String,
    val role: Role = Role.USER
) : DomainModel<UUID> {
    /** The limits that every new user must meet. */
    companion object {
        /** The maximum length of a login name. The column `users.login_name` has the same width. */
        const val MAX_LOGIN_NAME_LENGTH = 255

        /**
         * The regular expression that a whole login name must match. It allows one or more ASCII letters, digits,
         * and underscores.
         */
        const val LOGIN_NAME_PATTERN = "\\w+"

        /**
         * The regular expression that a whole email address must match. The address must have exactly one at sign with
         * at least one character on each side, and it must not contain whitespace. An address that matches therefore
         * has at least three characters, which the CHECK constraint on `users.email_address` requires as well.
         */
        const val EMAIL_ADDRESS_PATTERN = "[^@\\s]+@[^@\\s]+"

        /** The maximum length of an email address. The column `users.email_address` has the same width. */
        const val MAX_EMAIL_LENGTH = 254

        /**
         * The maximum length of a first or a last name. The columns `users.first_name` and `users.last_name` have the
         * same width.
         */
        const val MAX_NAME_LENGTH = 255
    }
}
