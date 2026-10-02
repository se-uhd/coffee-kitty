package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainers
import de.seuhd.campuscoffee.tests.SystemTestUtils.getPostgresContainer
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Single Spring and Cucumber configuration shared by all acceptance step definitions. Cucumber allows
 * only one [CucumberContextConfiguration], so the step classes hold step definitions only and rely on the
 * context, container, and cleanup hooks defined here. Each scenario starts from the fixture users, each with a
 * coffee count of zero.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
class CucumberSpringConfiguration(
    private val userService: UserService,
    private val dataResetService: DataResetService
) {
    @LocalServerPort
    private var port: Int = 0

    @Before
    fun beforeEach() {
        dataResetService.clearAll()
        TestFixtures.createUserFixtures(userService)
        configureClient(port)
    }

    @After
    fun afterEach() {
        dataResetService.clearAll()
    }

    companion object {
        // share one testcontainers instance across all acceptance tests
        private val postgresContainer: PostgreSQLContainer<*> = getPostgresContainer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainers(registry, postgresContainer)
        }
    }
}
