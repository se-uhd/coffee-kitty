package de.seuhd.campuscoffee.dev

import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.SmartInitializingSingleton

/**
 * Prepares the data of the dev application on every start. It deletes the rows of every table and then creates the
 * fixture users of [TestFixtures], each with a coffee count of zero. Each run of the dev application therefore begins
 * with the same data.
 *
 * Spring calls [afterSingletonsInstantiated] after Flyway has migrated the schema and before the embedded web server
 * accepts requests, so the fixture users exist before the first request.
 */
class DevDataSeeder(
    private val databaseCleaner: DatabaseCleaner,
    private val userInternalService: UserInternalService
) : SmartInitializingSingleton {
    override fun afterSingletonsInstantiated() {
        databaseCleaner.clean()
        val users = TestFixtures.createUserFixtures(userInternalService)
        log.info { "Cleaned the database and created the fixture users: ${users.size} users." }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
