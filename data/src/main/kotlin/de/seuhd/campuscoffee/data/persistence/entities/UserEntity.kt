package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Table

/**
 * JPA entity for a user. The companion object holds the names of the unique constraints that the Flyway migration
 * declares, for `ConstraintMapping`.
 */
@jakarta.persistence.Entity
@Table(name = "users")
class UserEntity : Entity() {
    @field:Column(name = "login_name")
    var loginName: String? = null

    @field:Column(name = "email_address")
    var emailAddress: String? = null

    @field:Column(name = "first_name")
    var firstName: String? = null

    @field:Column(name = "last_name")
    var lastName: String? = null

    companion object {
        /** Name of the unique constraint on the login name. */
        const val LOGIN_NAME_UNIQUE_CONSTRAINT = "uq_users_login_name"

        /** Name of the unique constraint on the email address. */
        const val EMAIL_ADDRESS_UNIQUE_CONSTRAINT = "uq_users_email_address"
    }
}
