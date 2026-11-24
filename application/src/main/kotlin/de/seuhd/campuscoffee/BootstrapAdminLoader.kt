package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.BootstrapAdminProperties
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Creates the bootstrap admin of [BootstrapAdminProperties] on startup when no admin exists, because only an admin may
 * create users. [UserInternalService.createInitialAdmin] checks a new admin like any other new user and creates the
 * consumption of the admin in the same transaction. If a user has the configured login name, that user becomes the
 * admin. This case occurs after the migration V4, which gives every existing user the role `USER`. The coffee app of
 * SE@UHD has a loader with the same name.
 *
 * [StartupDataInitializer] calls [createBootstrapAdmin] on startup. The dev application creates the fixture users, and
 * the fixture user `jane_doe` is an admin.
 */
@Component
class BootstrapAdminLoader(
    private val userInternalService: UserInternalService,
    private val bootstrapAdminProperties: BootstrapAdminProperties
) {
    /**
     * Creates the bootstrap admin unless the configuration has no login name or an admin already exists.
     *
     * @throws IllegalArgumentException if the configuration is incomplete or if the domain refuses the configured user.
     *   The configuration is incomplete if it has an email address but no login name, or a login name but no email
     *   address, first name, or last name.
     */
    fun createBootstrapAdmin() {
        val properties = bootstrapAdminProperties
        val loginName = properties.loginName
        if (loginName.isNullOrBlank()) {
            require(properties.emailAddress.isNullOrBlank()) {
                "$PREFIX.login-name must be set when $PREFIX.email-address is set."
            }
            log.info { "Skipped the bootstrap admin, because $PREFIX.login-name is not set." }
            return
        }
        val admin =
            User(
                loginName = loginName,
                emailAddress = required(properties.emailAddress, "email-address"),
                firstName = required(properties.firstName, "first-name"),
                lastName = required(properties.lastName, "last-name"),
                role = Role.ADMIN
            )
        val stored =
            try {
                createWithRetry(admin)
            } catch (e: ValidationException) {
                throw IllegalArgumentException("$PREFIX is invalid: ${e.message}", e)
            } catch (e: DuplicationException) {
                throw IllegalArgumentException("$PREFIX is invalid: ${e.message}", e)
            }
        if (stored == null) {
            log.info { "Skipped the bootstrap admin, because an admin already exists." }
        } else {
            log.info { "The bootstrap admin has the id ${stored.id}." }
        }
    }

    /**
     * Calls [UserInternalService.createInitialAdmin] and calls it a second time after a [DuplicationException]. Two
     * instances of the app that start on the same empty database at the same time both try to store the admin. The
     * second instance cannot insert the same login name, and its transaction rolls back. Its second call finds the
     * admin of the first instance and returns null. If the duplicate remains, such as the email address of another
     * user, the second call throws the exception again.
     */
    private fun createWithRetry(admin: User): User? =
        try {
            userInternalService.createInitialAdmin(admin)
        } catch (_: DuplicationException) {
            userInternalService.createInitialAdmin(admin)
        }

    /**
     * Returns [value], or throws an exception with the key under [PREFIX] in its message if [value] is missing or
     * blank.
     */
    private fun required(
        value: String?,
        key: String
    ): String =
        requireNotNull(value?.takeIf { it.isNotBlank() }) {
            "$PREFIX.$key must be set when $PREFIX.login-name is set."
        }

    private companion object {
        private val log = KotlinLogging.logger {}

        /** The prefix of the configuration keys of the bootstrap admin. */
        const val PREFIX = "campus-coffee.bootstrap-admin"
    }
}
