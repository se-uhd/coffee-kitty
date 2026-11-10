package de.seuhd.campuscoffee.domain.tests

import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService

/**
 * The five fixture users. The fixture loader and the tests use this object. It is in `src/main`, because the fixture
 * loader is production code and cannot use classes from `src/test`.
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

    /** Returns the fixture users, which have no ids or timestamps because they are not stored. */
    fun getUserFixtures(): List<User> = USER_LIST

    /**
     * Creates the fixture users through [userService] and returns the stored users. Each user gets a consumption with
     * a coffee count of zero.
     *
     * @param userService the port that creates the users
     */
    fun createUserFixtures(userService: UserService): List<User> = getUserFixtures().map { userService.create(it) }
}
