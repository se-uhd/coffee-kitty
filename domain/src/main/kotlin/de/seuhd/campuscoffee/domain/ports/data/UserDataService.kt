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
     * Returns the user with [loginName]. `ada` and `Ada` are different login names.
     *
     * @param loginName the login name to look up
     * @throws NotFoundException if no user has [loginName]
     */
    fun getByLoginName(loginName: String): User
}
