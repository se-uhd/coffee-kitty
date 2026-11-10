package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.Application
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base class of the system tests. The Spring Boot test context uses the shared PostgreSQL container and serves the API
 * on a random port. Before each test, [beforeEach] runs a data reset, creates the fixture users at a coffee count of
 * zero, and binds the shared RestTestClient to the server port. After each test, [afterEach] runs another data reset.
 */
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSystemTest {
    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var dataResetService: DataResetService

    @LocalServerPort
    private var port: Int = 0

    /** The fixture users that [beforeEach] created for the current test. */
    protected lateinit var fixtureUsers: List<User>

    @BeforeEach
    fun beforeEach() {
        dataResetService.clearAll()
        fixtureUsers = TestFixtures.createUserFixtures(userService)
        configureClient(port)
    }

    @AfterEach
    fun afterEach() {
        dataResetService.clearAll()
    }

    /** Returns the fixture user with [loginName]. */
    protected fun fixtureUser(loginName: String): User = fixtureUsers.first { it.loginName == loginName }

    protected companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainer(registry)
        }
    }
}
