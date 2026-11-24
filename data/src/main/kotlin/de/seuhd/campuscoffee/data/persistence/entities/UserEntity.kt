package de.seuhd.campuscoffee.data.persistence.entities

import de.seuhd.campuscoffee.domain.model.Role
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * JPA entity of a user. The companion object holds the names of the unique constraints that the migration
 * `V1__create_users_table.sql` declares. `ConstraintMapping` refers to the constraints by these names.
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

    /** The role of the user, stored as the name of the [Role] constant, such as `ADMIN` (migration V4). */
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "role")
    var role: Role? = null

    companion object {
        /** The name of the unique constraint on the login name. */
        const val LOGIN_NAME_UNIQUE_CONSTRAINT = "uq_users_login_name"

        /** The name of the unique constraint on the email address. */
        const val EMAIL_ADDRESS_UNIQUE_CONSTRAINT = "uq_users_email_address"
    }
}
