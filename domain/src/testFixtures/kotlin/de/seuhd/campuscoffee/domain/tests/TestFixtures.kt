package de.seuhd.campuscoffee.domain.tests

import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The test fixtures, that is, the test objects with fixed values that the tests share. They are the five fixture users,
 * the fixed time [FIXED_TIME] with a clock that shows it, and consumptions in three states. Only the fixture user
 * `jane_doe` is an admin. The tests of every module use these objects, and the dev application creates the fixture
 * users on every start. `TestFixtures` is in the source set `src/testFixtures` of the domain module. The other modules
 * add it to their test classpath, and the production jar does not contain it.
 */
object TestFixtures {
    /**
     * The fixed time of the test fixtures, 2026-11-16 at 10:00:00 UTC. [fixedClock] returns a clock that shows this
     * time, and the coffees of [consumptionWithRecentCoffee] and [consumptionWithOldCoffee] have times relative to
     * [FIXED_TIME]. A test with these fixtures does not depend on the time at which it runs.
     */
    val FIXED_TIME: LocalDateTime = LocalDateTime.parse("2026-11-16T10:00:00")

    private val USER_LIST =
        listOf(
            User(
                loginName = "jane_doe",
                emailAddress = "jane.doe@se.uni-heidelberg.de",
                firstName = "Jane",
                lastName = "Doe",
                role = Role.ADMIN
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

    /** Returns the fixture user who is an admin, `jane_doe`. The coffee app of SE@UHD has a function with this name. */
    fun admin(): User = getUserFixtures().first { it.role == Role.ADMIN }

    /**
     * Returns a fixture user who is not an admin, `maxmustermann`. The coffee app of SE@UHD has a function with this
     * name.
     */
    fun regularUser(): User = getUserFixtures().first { it.role == Role.USER }

    /**
     * Creates the fixture users through [userInternalService] and returns the stored users. Each user gets a
     * consumption with a coffee count of zero.
     *
     * @param userInternalService the port that creates the users without an acting user
     */
    fun createUserFixtures(userInternalService: UserInternalService): List<User> =
        getUserFixtures().map { userInternalService.create(it) }

    /**
     * Returns a clock in UTC that always shows [time]. A test passes this clock to a domain service in place of the
     * system clock to set the current time of the service.
     *
     * @param time the time that the clock shows, which is [FIXED_TIME] by default
     */
    fun fixedClock(time: LocalDateTime = FIXED_TIME): Clock =
        Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    /**
     * Returns the consumption of [user] without a coffee, as `UserService.create` stores it for a new user. Like the
     * fixture users, the three consumptions have no ids or timestamps, because they are not stored.
     *
     * @param user the user of the consumption
     */
    fun consumptionWithoutCoffee(user: User): CoffeeConsumption = CoffeeConsumption(user = user, count = 0)

    /**
     * Returns the consumption of [user] with one coffee, which the user added at [FIXED_TIME]. At [FIXED_TIME], the
     * coffee is within its grace period, and the user can undo it.
     *
     * @param user the user of the consumption
     */
    fun consumptionWithRecentCoffee(user: User): CoffeeConsumption =
        CoffeeConsumption(user = user, count = 1, coffeeTimes = listOf(FIXED_TIME))

    /**
     * Returns the consumption of [user] with one coffee, which the user added one hour before [FIXED_TIME]. At
     * [FIXED_TIME], the default grace period of 5 minutes has passed, and the user can no longer undo the coffee.
     *
     * @param user the user of the consumption
     */
    fun consumptionWithOldCoffee(user: User): CoffeeConsumption =
        CoffeeConsumption(user = user, count = 1, coffeeTimes = listOf(FIXED_TIME.minusHours(1)))
}
