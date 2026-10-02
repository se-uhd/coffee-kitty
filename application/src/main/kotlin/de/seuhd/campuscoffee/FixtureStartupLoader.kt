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
 * Loads the fixture users on startup when `campus-coffee.fixtures.load-on-startup` is true (declared by
 * [FixturesProperties]). Creating a user through [UserService.create] also creates the user's coffee consumption
 * at zero. The KDoc of [loadOnStartup] says when the loader clears the data first and when it loads nothing.
 *
 * As a [SmartInitializingSingleton], the loader runs at the end of singleton initialization, when Flyway has migrated
 * the schema and every bean is ready. For a servlet application that is before the embedded web server accepts
 * requests, so the API is never served before its data is loaded.
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
     * Loads the fixture users. When `reset-on-startup` is set, as in the dev profile, the loader first clears all
     * data and then creates the fixture users, so each run starts with them at a coffee count of zero. Otherwise, the
     * loader creates the fixture users only when the database has no users yet.
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
