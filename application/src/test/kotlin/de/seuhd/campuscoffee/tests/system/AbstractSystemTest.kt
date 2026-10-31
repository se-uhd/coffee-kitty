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
 * Base class of the system tests. It sets up the Spring Boot test context on the shared PostgreSQL container,
 * creates the fixture users (each with a coffee count of zero) before each test, and binds the shared
 * RestTestClient.
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

    /** The fixture users created before each test, available so a test can act on a specific one. */
    protected lateinit var fixtureUsers: List<User>

    @BeforeEach
    fun beforeEach() {
        dataResetService.clearAll()
        // create the fixture users, each together with a consumption at zero
        fixtureUsers = TestFixtures.createUserFixtures(userService)
        configureClient(port)
    }

    @AfterEach
    fun afterEach() {
        dataResetService.clearAll()
    }

    /** The fixture user with the given login name. */
    protected fun fixtureUser(loginName: String): User = fixtureUsers.first { it.loginName == loginName }

    protected companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainer(registry)
        }
    }
}
