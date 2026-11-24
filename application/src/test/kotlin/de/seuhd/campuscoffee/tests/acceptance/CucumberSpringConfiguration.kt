package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import de.seuhd.campuscoffee.tests.SystemTestUtils
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainer
import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource

/**
 * The Spring context and the hook for every acceptance scenario. Cucumber accepts only one class with
 * [CucumberContextConfiguration], and the step classes therefore contain only step definitions. The Spring context uses
 * the same PostgreSQL container as the system tests. Before each scenario, [beforeEach] deletes the rows of every table
 * with the [DatabaseCleaner] and creates the user fixtures with a coffee count of zero. The nested
 * [ScenarioConfiguration] adds the [SettableClock], which the step definitions set to the times in the scenarios, and
 * the [ConsumptionRequests], through which they send their requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
class CucumberSpringConfiguration(
    private val userInternalService: UserInternalService,
    private val dataSource: DataSource
) {
    @Before
    fun beforeEach() {
        DatabaseCleaner(dataSource).clean()
        TestFixtures.createUserFixtures(userInternalService)
    }

    /**
     * Adds the [SettableClock] and the [ConsumptionRequests] to the Spring context of the scenarios. `@SpringBootTest`
     * uses a nested `@TestConfiguration` in addition to the configuration of the application.
     */
    @TestConfiguration
    class ScenarioConfiguration {
        /** Returns the clock of the scenarios. Spring picks the `@Primary` clock wherever the app injects a `Clock`. */
        @Bean
        @Primary
        fun settableClock(): SettableClock = SettableClock()

        /**
         * Returns the requests for the server of this context. The bean is lazy, because the port is available only
         * after the embedded web server has started. Cucumber creates the step classes, which need the bean, for each
         * scenario and therefore after the start.
         */
        @Bean
        @Lazy
        fun consumptionRequests(
            @Value("\${local.server.port}") port: Int
        ): ConsumptionRequests = ConsumptionRequests(SystemTestUtils.clientFor(port))
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainer(registry)
        }
    }
}
