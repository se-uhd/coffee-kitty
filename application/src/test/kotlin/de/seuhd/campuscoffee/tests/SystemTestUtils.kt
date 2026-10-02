package de.seuhd.campuscoffee.tests

import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Utilities for the system and acceptance tests: the PostgreSQL container wiring, the shared [RestTestClient], and
 * the status helpers.
 */
object SystemTestUtils {
    private lateinit var client: RestTestClient

    /** Binds the shared [RestTestClient] to the running server on the given port. */
    fun configureClient(port: Int) {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    /** The client bound to the running server. */
    fun client(): RestTestClient = client

    // Creates a PostgreSQL testcontainer. The container is AutoCloseable but deliberately not closed here:
    // Callers keep it open for the whole test run, and Testcontainers stops it on JVM shutdown.
    @Suppress("resource")
    fun getPostgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:18-alpine"))

    /** Points the Spring datasource at the given PostgreSQL testcontainer. */
    fun configurePostgresContainers(
        registry: DynamicPropertyRegistry,
        postgresContainer: PostgreSQLContainer<*>
    ) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
        registry.add("spring.datasource.username", postgresContainer::getUsername)
        registry.add("spring.datasource.password", postgresContainer::getPassword)
    }

    /** The status code of a response, without asserting it. */
    fun RestTestClient.ResponseSpec.statusCode(): Int = returnResult<ByteArray>().status.value()

    /**
     * Requires that a setup request answered [expected], so a failed step cannot leave a later assertion vacuous.
     *
     * @param expected the status code of a successful setup request
     */
    fun RestTestClient.ResponseSpec.requireStatus(expected: Int) {
        assertThat(statusCode()).`as`("setup request status").isEqualTo(expected)
    }
}
