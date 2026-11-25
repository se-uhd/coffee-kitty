package de.seuhd.campuscoffee.dev

import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * Adds the [DevFixtureLoader] to the dev application. The component scan of a Spring Boot application leaves out a
 * `@TestConfiguration`, so no test context contains the fixture loader. Only `main` in `DevApplication.kt` registers
 * this class.
 */
@TestConfiguration(proxyBeanMethods = false)
class DevDataConfiguration {
    /** Returns the fixture loader, which cleans the database with [databaseCleaner] and creates the user fixtures. */
    @Bean
    fun devFixtureLoader(
        databaseCleaner: DatabaseCleaner,
        userInternalService: UserInternalService
    ): DevFixtureLoader = DevFixtureLoader(databaseCleaner, userInternalService)
}
