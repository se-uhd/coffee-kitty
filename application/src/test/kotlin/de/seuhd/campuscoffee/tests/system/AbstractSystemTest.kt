package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.Application
import de.seuhd.campuscoffee.data.tests.DatabaseCleaner
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import de.seuhd.campuscoffee.tests.SystemTestUtils
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainer
import de.seuhd.campuscoffee.tests.UserRequests
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import javax.sql.DataSource

/**
 * Base class of the system tests. The Spring Boot test context uses the shared PostgreSQL container and serves the API
 * on a random port. Before each test, [beforeEach] deletes the rows of every table with the [DatabaseCleaner], creates
 * the fixture users at a coffee count of zero, and creates [userRequests] and [consumptionRequests] for the port of
 * this context.
 */
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSystemTest {
    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var userInternalService: UserInternalService

    @Autowired
    private lateinit var dataSource: DataSource

    @LocalServerPort
    private var port: Int = 0

    /** The fixture users that [beforeEach] created for the current test. */
    protected lateinit var fixtureUsers: List<User>

    /** The client of the server of this context, for a test that builds a request itself. */
    protected lateinit var client: RestTestClient

    /** Sends the requests for users to the server of this context. */
    protected lateinit var userRequests: UserRequests

    /** Sends the requests for coffee counts to the server of this context. */
    protected lateinit var consumptionRequests: ConsumptionRequests

    @BeforeEach
    fun beforeEach() {
        DatabaseCleaner(dataSource).clean()
        fixtureUsers = TestFixtures.createUserFixtures(userInternalService)
        client = SystemTestUtils.clientFor(port)
        userRequests = UserRequests(client)
        consumptionRequests = ConsumptionRequests(client)
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
