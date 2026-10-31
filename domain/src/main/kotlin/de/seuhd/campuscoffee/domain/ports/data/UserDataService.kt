package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import java.util.UUID

/**
 * Data port for the users, implemented by `UserDataServiceImpl` in the data module. Its [getAll] returns the users
 * ordered by login name.
 */
interface UserDataService : CrudDataService<User, UUID> {
    /**
     * Retrieves the user with the given login name. No production code calls it yet, only
     * `CrudDataServiceIntegrationTest`. It is there for an extension that looks up a user by login name, such as a
     * sign-in that maps the authenticated login name to a user, as in the coffee app of SE@UHD.
     *
     * @param loginName the login name of the user to retrieve
     * @return the user with [loginName]
     * @throws NotFoundException if no user has [loginName]
     */
    fun getByLoginName(loginName: String): User
}
