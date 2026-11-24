package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainer
import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource

/**
 * The Spring context and the hooks for every acceptance scenario. Cucumber accepts only one class with
 * [CucumberContextConfiguration], and the step classes therefore contain only step definitions. The Spring context uses
 * the same PostgreSQL container as the system tests. Before each scenario, [beforeEach] deletes the rows of every table
 * with the [DatabaseCleaner] and creates the fixture users at a coffee count of zero. The nested [TestClockConfiguration] adds a [SettableClock], which the
 * step definitions set to the times in the scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
class CucumberSpringConfiguration(
    private val userInternalService: UserInternalService,
    private val dataSource: DataSource
) {
    @LocalServerPort
    private var port: Int = 0

    @Before
    fun beforeEach() {
        DatabaseCleaner(dataSource).clean()
        TestFixtures.createUserFixtures(userInternalService)
        configureClient(port)
    }

    /**
     * Adds the [SettableClock] to the Spring context of the scenarios. `@SpringBootTest` uses a nested
     * `@TestConfiguration` in addition to the configuration of the application. Spring picks the `@Primary` clock
     * wherever the application injects a `Clock`.
     */
    @TestConfiguration
    class TestClockConfiguration {
        @Bean
        @Primary
        fun settableClock(): SettableClock = SettableClock()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainer(registry)
        }
    }
}
