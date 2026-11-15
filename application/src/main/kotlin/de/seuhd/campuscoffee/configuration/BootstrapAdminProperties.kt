package de.seuhd.campuscoffee.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The bootstrap admin, bound from the keys under `campus-coffee.bootstrap-admin`. On every start,
 * `BootstrapAdminLoader` creates this user as an admin when no admin exists, so that a database without the fixture
 * user `jane_doe` gets an admin who can create users. The coffee app of SE@UHD has a class with the same name, which
 * also holds a password.
 *
 * @property loginName    the login name of the admin. Without a login name, the app creates no bootstrap admin.
 * @property emailAddress the email address of the admin, required with a login name
 * @property firstName    the first name of the admin, required with a login name
 * @property lastName     the last name of the admin, required with a login name
 */
@ConfigurationProperties("campus-coffee.bootstrap-admin")
data class BootstrapAdminProperties(
    val loginName: String? = null,
    val emailAddress: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
