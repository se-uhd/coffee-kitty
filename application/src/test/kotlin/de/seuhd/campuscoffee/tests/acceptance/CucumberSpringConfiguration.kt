package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainer
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * The Spring context and the hooks for every acceptance scenario. Cucumber accepts only one class with
 * [CucumberContextConfiguration], and the step classes therefore contain only step definitions. The Spring context
 * uses the same PostgreSQL container as the system tests. Before each scenario, [beforeEach] runs a data reset and
 * creates the fixture users at a coffee count of zero.
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
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainer(registry)
        }
    }
}
