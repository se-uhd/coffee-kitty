package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model of a person whose coffees the app counts. Instances are immutable. The constants in the companion
 * object are the limits for a new user. `UserServiceImpl.create` checks these limits for every new user, including
 * the fixture users. It also refuses a blank name and a name with a control character. Bean Validation on `UserDto`
 * checks the same limits before a request reaches the domain, but it checks the email address with `@Email` instead
 * of [EMAIL_ADDRESS_PATTERN].
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a user that is not stored yet.
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
        /** The maximum length of a login name. The column `users.login_name` has the same width. */
        const val MAX_LOGIN_NAME_LENGTH = 255

        /**
         * The regular expression that a whole login name must match. It allows one or more ASCII letters, digits,
         * and underscores.
         */
        const val LOGIN_NAME_PATTERN = "\\w+"

        /**
         * The regular expression that a whole email address must match. The address needs exactly one at sign, at
         * least one character on each side of the at sign, and no whitespace. The `@Email` check on `UserDto`
         * refuses some addresses that this pattern accepts, such as `a,b@se.de` and `a..b@se.de`. Conversely, it
         * accepts a quoted local part such as `"jane doe"@se.de`, which this pattern refuses.
         */
        const val EMAIL_ADDRESS_PATTERN = "[^@\\s]+@[^@\\s]+"

        /**
         * The minimum length of an email address, which the check constraint on `users.email_address` requires.
         * `UserDto` checks this length with `@Size`, because `@Email` accepts an empty string. The domain does not
         * check the length, because every address that matches [EMAIL_ADDRESS_PATTERN] has at least three characters.
         */
        const val MIN_EMAIL_LENGTH = 3

        /** The maximum length of an email address. The column `users.email_address` has the same width. */
        const val MAX_EMAIL_LENGTH = 254

        /**
         * The maximum length of a first or a last name. The columns `users.first_name` and `users.last_name` have the
         * same width.
         */
        const val MAX_NAME_LENGTH = 255
    }
}
