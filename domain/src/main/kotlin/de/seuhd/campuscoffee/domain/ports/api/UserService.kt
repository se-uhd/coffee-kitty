package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import java.util.UUID

/**
 * API port for the users, implemented in the domain module and called by the controllers. Its implementation checks the
 * rules for a new user (see [create]) and stores users through the [UserDataService] port. The fixture loader and the
 * test setup create the fixture users through [create], as the API does.
 */
interface UserService {
    /**
     * Lists every user.
     *
     * @return every user, ordered by login name
     */
    fun getAll(): List<User>

    /**
     * Retrieves a user by id.
     *
     * @param id the id of the user to retrieve
     * @return the user with [id]
     * @throws NotFoundException if no user exists with [id]
     */
    fun getById(id: UUID): User

    /**
     * Creates a new user together with their [de.seuhd.campuscoffee.domain.model.CoffeeConsumption] at a coffee count
     * of zero. The service ignores any id and timestamps on [user], trims and lower-cases the email address, checks
     * the login name, the normalized email address, and the first and the last name against the limits in the companion
     * object of [User], and refuses a blank name or one with a control character.
     *
     * @param user the user to create
     * @return the stored user with its assigned id and timestamps
     * @throws ValidationException if the login name, the email address, or a name breaks these rules
     * @throws DuplicationException if a user already holds the login name or the email address
     */
    fun create(user: User): User
}
