package de.seuhd.campuscoffee.domain.ports.api

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import java.util.UUID

/**
 * Service interface for user operations.
 *
 * This is a port in the hexagonal architecture pattern, implemented by the domain layer and consumed by
 * the API layer. It encapsulates business rules and coordinates data operations through the
 * [UserDataService] port. The fixture loader and test setup create the fixture users through [create], as the API
 * does.
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
     * Creates a new user together with their [de.seuhd.campuscoffee.domain.model.CoffeeConsumption] at
     * `count = 0`. The service ignores any id on [user], trims and lower-cases the email address, and checks the
     * login name and the email address against the rules in [User]'s companion object.
     *
     * @param user the user to create
     * @return the persisted user with its assigned id and timestamps
     * @throws ValidationException if the login name or the email address breaks the rules in [User]'s companion
     *   object
     * @throws DuplicationException if a user already holds the login name or the email address
     */
    fun create(user: User): User
}
