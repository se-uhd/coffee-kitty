package de.seuhd.campuscoffee.data.integration

import de.seuhd.campuscoffee.data.DataTestApplication
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Base class for data layer integration tests. Boots the data layer against a real PostgreSQL container with the
 * Flyway-managed schema and clears all data through [DataResetService] before each test, as the system tests do. At
 * startup, Hibernate checks that the entities match that schema (`ddl-auto: validate` in the test
 * `application.yaml`).
 */
@SpringBootTest(classes = [DataTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class AbstractDataIntegrationTest {
    @Autowired
    private lateinit var dataResetService: DataResetService

    @BeforeEach
    fun clearDatabase() {
        dataResetService.clearAll()
    }

    companion object {
        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))

        init {
            postgresContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }
}
