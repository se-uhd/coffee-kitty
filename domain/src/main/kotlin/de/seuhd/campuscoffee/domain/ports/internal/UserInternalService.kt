package de.seuhd.campuscoffee.domain.ports.internal

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User

/**
 * The internal port for creating a user without an acting user. The test setup and the dev application create the
 * user fixtures through it. The controllers create users through `UserService.create`, which also checks that the
 * acting user is an admin. An architecture test fails if a class in the api module uses this port. The coffee app of
 * SE@UHD has a port with the same name.
 */
interface UserInternalService {
    /**
     * Creates [user] with the checks of `UserService.create`, but without an acting user.
     *
     * @param user the user to create
     * @return the stored user with the id and the timestamps that the data module assigned
     * @throws ValidationException if the login name, the email address, or a name breaks a rule of `UserService.create`
     * @throws DuplicationException if another user has the login name or the email address
     */
    fun create(user: User): User

    /**
     * Stores [admin] with the role `ADMIN` when no user has this role yet. If a user has the login name of [admin], the
     * function gives that user the role `ADMIN` and changes no other field. Otherwise, it creates [admin] with the
     * checks of `UserService.create`. The bootstrap admin loader calls this function on startup.
     *
     * @param admin the admin to create
     * @return the stored admin, or null if an admin already exists
     * @throws ValidationException if the login name, the email address, or a name breaks a rule of `UserService.create`
     * @throws DuplicationException if another user has the email address, or if a concurrent transaction stores a user
     *   with the login name
     */
    fun createInitialAdmin(admin: User): User?
}
