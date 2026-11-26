package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of a person whose coffees the app counts. Instances are immutable. The Bean Validation constraints
 * on the properties are the limits for a new user, and the constants in the companion object hold their values and
 * messages. `UserServiceImpl.create` checks the constraints with `DomainValidator` for every new user, including the
 * user fixtures and the bootstrap admin. The api module checks only that a request contains every field.
 * `CreateUserDto` repeats the limits in `@Schema` annotations to document them in the OpenAPI definition.
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a user that is not stored yet. [role]
 * determines which actions the user may perform, and it is [Role.USER] by default.
 */
data class User(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @field:Size(max = MAX_LOGIN_NAME_LENGTH, message = LOGIN_NAME_MESSAGE)
    @field:Pattern(regexp = LOGIN_NAME_PATTERN, message = LOGIN_NAME_MESSAGE)
    val loginName: String,
    @field:Size(max = MAX_EMAIL_LENGTH, message = EMAIL_ADDRESS_MESSAGE)
    @field:Pattern(regexp = EMAIL_ADDRESS_PATTERN, message = EMAIL_ADDRESS_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_CONTROL_CHARACTER, message = EMAIL_ADDRESS_MESSAGE)
    val emailAddress: String,
    @field:Pattern(regexp = TextPatterns.NOT_BLANK, message = FIRST_NAME_MESSAGE)
    @field:Size(max = MAX_NAME_LENGTH, message = FIRST_NAME_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_CONTROL_CHARACTER, message = FIRST_NAME_MESSAGE)
    val firstName: String,
    @field:Pattern(regexp = TextPatterns.NOT_BLANK, message = LAST_NAME_MESSAGE)
    @field:Size(max = MAX_NAME_LENGTH, message = LAST_NAME_MESSAGE)
    @field:Pattern(regexp = TextPatterns.NO_CONTROL_CHARACTER, message = LAST_NAME_MESSAGE)
    val lastName: String,
    val role: Role = Role.USER
) : DomainModel<UUID> {
    /** The limits that every new user must meet. */
    companion object {
        /**
         * The maximum length of a login name, which the team assistant set (FR-14 in
         * `requirements/FR-14-create-user.md`). The column `users.login_name` has the same width.
         */
        const val MAX_LOGIN_NAME_LENGTH = 255

        /**
         * The regular expression that a whole login name must match. It allows one or more ASCII letters, digits,
         * and underscores.
         */
        const val LOGIN_NAME_PATTERN = "\\w+"

        /**
         * The regular expression that a whole email address must match. The address must have exactly one at sign with
         * at least one character on each side, and it must not contain whitespace. An address that matches therefore
         * has at least three characters, which the CHECK constraint on `users.email_address` requires as well. A second
         * constraint rejects the control characters, as in a name: RFC 5321, section 4.1.2, allows no ASCII control
         * character in an address.
         */
        const val EMAIL_ADDRESS_PATTERN = "[^@\\s]+@[^@\\s]+"

        /**
         * The maximum length of an email address that a mail server accepts. RFC 5321, section 4.5.3.1.3, limits a
         * path in an SMTP command to 256 characters, which include the two angle brackets around the address (see
         * Erratum 1690 to RFC 3696). The column `users.email_address` has the same width.
         */
        const val MAX_EMAIL_LENGTH = 254

        /**
         * The maximum length of a first or a last name. No stakeholder set this limit, which is an open TBD in FR-14.
         * Until the answer, the limit is the width of the columns `users.first_name` and `users.last_name`, so that
         * no request can store a text of any length. `@Size` counts UTF-16 code units, so a character outside the
         * Basic Multilingual Plane counts as two.
         */
        const val MAX_NAME_LENGTH = 255

        /** The message of the error response for an invalid login name. */
        const val LOGIN_NAME_MESSAGE =
            "A login name must have 1 to $MAX_LOGIN_NAME_LENGTH characters, each an ASCII letter, a digit, or an " +
                "underscore."

        /** The message of the error response for an invalid email address. */
        const val EMAIL_ADDRESS_MESSAGE =
            "An email address must have the form name@domain, with one at sign and no spaces or control characters, " +
                "and at most $MAX_EMAIL_LENGTH characters."

        /** The message of the error response for an invalid first name. */
        const val FIRST_NAME_MESSAGE =
            "A first name must have 1 to $MAX_NAME_LENGTH characters, must not be blank, and must not contain a " +
                "control character."

        /** The message of the error response for an invalid last name. */
        const val LAST_NAME_MESSAGE =
            "A last name must have 1 to $MAX_NAME_LENGTH characters, must not be blank, and must not contain a " +
                "control character."
    }
}
