package de.seuhd.campuscoffee.domain.tests

import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService

/**
 * Test and demo fixtures: the five users, each created with a coffee consumption at zero. The object is in
 * `src/main` so that the startup loader and the tests share one source of fixture data.
 */
object TestFixtures {
    private val USER_LIST =
        listOf(
            User(
                loginName = "jane_doe",
                emailAddress = "jane.doe@se.uni-heidelberg.de",
                firstName = "Jane",
                lastName = "Doe"
            ),
            User(
                loginName = "maxmustermann",
                emailAddress = "max.mustermann@se.uni-heidelberg.de",
                firstName = "Max",
                lastName = "Mustermann"
            ),
            User(
                loginName = "student2023",
                emailAddress = "student2023@se.uni-heidelberg.de",
                firstName = "Student",
                lastName = "Example"
            ),
            User(
                loginName = "lisa_lee",
                emailAddress = "lisa.lee@se.uni-heidelberg.de",
                firstName = "Lisa",
                lastName = "Lee"
            ),
            User(
                loginName = "olivia_lee",
                emailAddress = "olivia.lee@se.uni-heidelberg.de",
                firstName = "Olivia",
                lastName = "Lee"
            )
        )

    /** Returns the fixture users, without ids or timestamps, ready for [createUserFixtures]. */
    fun getUserFixtures(): List<User> = USER_LIST

    /**
     * Creates the fixture users through [userService], which also creates each user's coffee consumption at zero,
     * and returns the stored users.
     *
     * @param userService the port that creates the users
     */
    fun createUserFixtures(userService: UserService): List<User> = getUserFixtures().map { userService.create(it) }
}
