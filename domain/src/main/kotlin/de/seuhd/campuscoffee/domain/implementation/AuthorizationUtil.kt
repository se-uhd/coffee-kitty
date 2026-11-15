package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User

/**
 * Requires that [actingUser] is an admin. Otherwise, the function throws a [ForbiddenException] (403) with the message
 * "Only an admin may [action]." The coffee app of SE@UHD has the same function.
 *
 * @param actingUser the user who sends the request
 * @param action completes the message "Only an admin may ...", such as "create a user"
 * @throws ForbiddenException if [actingUser] is not an admin
 */
internal fun requireAdmin(
    actingUser: User,
    action: String
) {
    if (actingUser.role != Role.ADMIN) {
        throw ForbiddenException("Only an admin may $action.")
    }
}
