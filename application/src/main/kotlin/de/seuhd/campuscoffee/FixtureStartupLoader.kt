package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.FixturesProperties
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Creates the fixture users on startup when `campus-coffee.fixtures.load-on-startup` is true (see
 * [FixturesProperties]). [UserService.create] creates each fixture user and a consumption for that user with a coffee
 * count of zero. The KDoc of [loadOnStartup] describes when the fixture loader runs a data reset first and when it
 * creates no users.
 *
 * Spring calls [afterSingletonsInstantiated] after it has created every singleton bean. At that point, Flyway has
 * migrated the schema, and the embedded web server does not accept requests yet. The app therefore serves no request
 * before the fixture users exist.
 */
@Component
@ConditionalOnProperty("campus-coffee.fixtures.load-on-startup", havingValue = "true")
class FixtureStartupLoader(
    private val userService: UserService,
    private val dataResetService: DataResetService,
    private val fixturesProperties: FixturesProperties
) : SmartInitializingSingleton {
    override fun afterSingletonsInstantiated() = loadOnStartup()

    /**
     * Creates the fixture users. When `reset-on-startup` is true, as in the dev profile, the fixture loader first runs
     * a data reset, and every run starts with the fixture users at a coffee count of zero. Otherwise, the fixture
     * loader creates the fixture users only in a database without users.
     */
    fun loadOnStartup() {
        if (fixturesProperties.resetOnStartup) {
            dataResetService.clearAll()
            val users = TestFixtures.createUserFixtures(userService)
            log.info { "Cleared the data and loaded the fixture users on startup: ${users.size} users." }
            return
        }
        if (userService.getAll().isNotEmpty()) {
            log.info { "Skipped loading the fixture users, because the database already has users." }
            return
        }
        val users = TestFixtures.createUserFixtures(userService)
        log.info { "Loaded the fixture users on startup: ${users.size} users." }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
