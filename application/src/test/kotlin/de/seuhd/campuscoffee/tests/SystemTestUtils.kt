package de.seuhd.campuscoffee.tests

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.web.servlet.client.RestTestClient
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.ConcurrentHashMap

/**
 * Utilities for the system and acceptance tests: the shared PostgreSQL container and the [RestTestClient] of each
 * server port.
 */
object SystemTestUtils {
    /** The header with the login name of the acting user, as a client sends it. */
    const val ACTING_USER_HEADER = "X-Acting-User"

    /**
     * The challenge in the header `WWW-Authenticate` of every 401, as a client receives it. RFC 9110 requires the
     * header in a 401.
     */
    const val AUTHENTICATION_CHALLENGE = "X-Acting-User realm=\"Coffee Kitty\""

    /** The login name of the fixture user who is an admin. */
    const val ADMIN_LOGIN_NAME = "jane_doe"

    private val clients = ConcurrentHashMap<Int, RestTestClient>()

    /**
     * Returns the [RestTestClient] for the server on [port]. Each Spring context of the tests serves the API on a port
     * of its own, and a test passes the port of its context. The function creates one client per port and never binds
     * a client to another port.
     *
     * @param port the port of the server, which `@LocalServerPort` injects
     */
    fun clientFor(port: Int): RestTestClient =
        clients.computeIfAbsent(port) { RestTestClient.bindToServer().baseUrl("http://localhost:$it").build() }

    // One PostgreSQL container for all system and acceptance tests in the test JVM. The container starts on first
    // use, and Testcontainers stops it when the JVM shuts down. The tests therefore never close the container,
    // although PostgreSQLContainer is AutoCloseable.
    @Suppress("resource")
    private val postgresContainer: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).apply { start() }
    }

    /** Points the Spring datasource at the shared PostgreSQL container. */
    fun configurePostgresContainer(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
        registry.add("spring.datasource.username", postgresContainer::getUsername)
        registry.add("spring.datasource.password", postgresContainer::getPassword)
    }
}
