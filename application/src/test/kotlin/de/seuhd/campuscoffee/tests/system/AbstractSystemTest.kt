package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.Application
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainers
import de.seuhd.campuscoffee.tests.SystemTestUtils.getPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Abstract base class for system tests. Sets up the Spring Boot test context, manages the PostgreSQL
 * testcontainer, creates the fixture users (each with a coffee count of zero) before each test, and binds the
 * shared RestTestClient.
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
    protected lateinit var seededUsers: List<User>

    @BeforeEach
    fun beforeEach() {
        dataResetService.clearAll()
        // create the fixture users, each together with a coffee consumption at zero
        seededUsers = TestFixtures.createUserFixtures(userService)
        configureClient(port)
    }

    @AfterEach
    fun afterEach() {
        dataResetService.clearAll()
    }

    /** The fixture user with the given login name. */
    protected fun seededUser(loginName: String): User = seededUsers.first { it.loginName == loginName }

    protected companion object {
        // Shared across all system tests, because a val in the companion object is a single instance, started once.
        protected val postgresContainer: PostgreSQLContainer<*> = getPostgresContainer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainers(registry, postgresContainer)
        }
    }
}
