package de.seuhd.campuscoffee.tests

import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Utilities for the system and acceptance tests: the shared PostgreSQL container, the shared [RestTestClient], and
 * the status helpers.
 */
object SystemTestUtils {
    private lateinit var client: RestTestClient

    /** Binds the shared [RestTestClient] to the server on [port]. */
    fun configureClient(port: Int) {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    /** Returns the shared [RestTestClient], which [configureClient] has bound to the server. */
    fun client(): RestTestClient = client

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

    /** Returns the status code of the response without asserting it. */
    fun RestTestClient.ResponseSpec.statusCode(): Int = returnResult<ByteArray>().status.value()

    /**
     * Asserts that a setup request returned the status [expected]. A failed setup step then stops the test before a
     * later assertion can pass for the wrong reason.
     *
     * @param expected the status code of a successful setup request
     */
    fun RestTestClient.ResponseSpec.requireStatus(expected: Int) {
        assertThat(statusCode()).`as`("setup request status").isEqualTo(expected)
    }
}
