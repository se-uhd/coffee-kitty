package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Immutable user domain model: a person whose coffees the app counts. The limits for a new user are the constants in
 * the companion object. `UserServiceImpl.create` checks them on every new user, the fixture users included, and also
 * refuses a blank name or one with a control character. Bean Validation on `UserDto` checks the same limits first,
 * except that it checks the email address with `@Email` instead of [EMAIL_ADDRESS_PATTERN].
 *
 * The data layer assigns [id], [createdAt], and [updatedAt], so they are null on a user that is not stored yet.
 */
data class User(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val loginName: String,
    val emailAddress: String,
    val firstName: String,
    val lastName: String
) : DomainModel<UUID> {
    /** The limits that every new user must meet. */
    companion object {
        /** The maximum length of a login name, the width of the `users.login_name` column. */
        const val MAX_LOGIN_NAME_LENGTH = 255

        /**
         * The characters that a login name may use, as a full-string regular expression: ASCII letters,
         * digits, and the underscore. It also rules out an empty login name.
         */
        const val LOGIN_NAME_PATTERN = "\\w+"

        /**
         * The form that the domain requires of an email address, as a full-string regular expression: one at
         * sign with at least one character on each side and no whitespace anywhere. It is not the API's `@Email`
         * rule, which also refuses, for example, a comma or two dots in a row, and which accepts a quoted local part
         * such as `"jane doe"@se.de`.
         */
        const val EMAIL_ADDRESS_PATTERN = "[^@\\s]+@[^@\\s]+"

        /**
         * The minimum length of an email address, as the `users.email_address` check constraint requires. The
         * `UserDto` checks it, because `@Email` accepts an empty string. [EMAIL_ADDRESS_PATTERN] implies it.
         */
        const val MIN_EMAIL_LENGTH = 3

        /** The maximum length of an email address, the width of the `users.email_address` column. */
        const val MAX_EMAIL_LENGTH = 254

        /** The maximum length of a first or a last name, the width of the `first_name` and `last_name` columns. */
        const val MAX_NAME_LENGTH = 255
    }
}
