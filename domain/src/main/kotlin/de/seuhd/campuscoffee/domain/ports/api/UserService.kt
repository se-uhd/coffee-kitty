package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import java.util.UUID

/**
 * The API port for the users. The controllers call it. `UserServiceImpl` in the domain module implements it and stores
 * the users through the data port [UserDataService]. The tests and the dev application create the user fixtures through
 * the internal port `UserInternalService`, which checks them like any other new user.
 */
interface UserService {
    /** Returns every user, ordered by login name. */
    fun getAll(): List<User>

    /**
     * Returns the user with [id].
     *
     * @param id the id of the user
     * @throws NotFoundException if no user has [id]
     */
    fun getById(id: UUID): User

    /**
     * Creates [user] and the user's consumption with a coffee count of zero. The service ignores any id and timestamps
     * on [user], stores the email address trimmed and in lowercase, and checks the constraints on [User].
     *
     * @param user the user to create
     * @param actingUser the user who sends the request
     * @return the stored user with the id and the timestamps that the data module assigned
     * @throws ForbiddenException if [actingUser] is not an admin
     * @throws ValidationException if a value of [user] breaks a constraint on [User]
     * @throws DuplicationException if another user has the login name or the email address
     */
    fun create(
        user: User,
        actingUser: User
    ): User

    /**
     * Returns the user with [loginName]. The api module looks up the acting user of a request by login name.
     *
     * @param loginName the login name of the user
     * @throws NotFoundException if no user has [loginName]
     */
    fun getByLoginName(loginName: String): User
}
