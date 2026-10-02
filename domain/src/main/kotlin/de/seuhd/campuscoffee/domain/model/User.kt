package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Immutable user domain model: an SE@UHD user whose coffees the app counts. The identity rules for the login name
 * and the email address are the constants in the companion object. The API's DTO constraints use the same limits as
 * a first check, with Bean Validation's stricter `@Email` for the address, and `UserServiceImpl.create` enforces them
 * on every new user, which covers the fixtures that bypass the API.
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
    /** The identity rules that every new user must meet. */
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
         * DTOs check it, because `@Email` accepts an empty string. [EMAIL_ADDRESS_PATTERN] implies it.
         */
        const val MIN_EMAIL_LENGTH = 3

        /** The maximum length of an email address, the width of the `users.email_address` column. */
        const val MAX_EMAIL_LENGTH = 254
    }
}
