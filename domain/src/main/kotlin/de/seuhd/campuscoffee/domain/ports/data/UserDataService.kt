package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import java.util.UUID

/**
 * Data port for the users. `UserDataServiceImpl` in the data module implements it. [getAll] returns the users ordered
 * by login name.
 */
interface UserDataService : CrudDataService<User, UUID> {
    /**
     * Returns the user with [loginName]. Only `CrudDataServiceIntegrationTest` calls this method so far. The method
     * exists for an extension that looks up users by login name, such as a sign-in that finds the user for an
     * authenticated login name. The coffee app of SE@UHD has such a sign-in.
     *
     * @param loginName the login name to look up
     * @throws NotFoundException if no user has [loginName]
     */
    fun getByLoginName(loginName: String): User
}
