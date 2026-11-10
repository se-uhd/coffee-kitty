package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import java.util.UUID

/**
 * API port for the users. The controllers call it. `UserServiceImpl` in the domain module implements it and stores
 * the users through the data port [UserDataService]. The fixture loader and the tests create the fixture users
 * through [create], which checks them like any other new user.
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
     * on [user] and stores the email address trimmed and in lowercase. It checks the login name, the email address,
     * and the first and the last name against the limits in the companion object of [User]. It also refuses a blank
     * name and a name with a control character.
     *
     * @param user the user to create
     * @return the stored user with the id and the timestamps that the data module assigned
     * @throws ValidationException if the login name, the email address, or a name breaks one of these rules
     * @throws DuplicationException if another user has the login name or the email address
     */
    fun create(user: User): User
}
