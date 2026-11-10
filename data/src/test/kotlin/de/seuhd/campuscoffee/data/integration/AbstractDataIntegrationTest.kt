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
 * Base class of the integration tests of the data module. Each test runs in the Spring context of
 * [DataTestApplication], with a PostgreSQL database in a Docker container from Testcontainers. At startup, Flyway
 * applies the migrations, and Hibernate then checks that the JPA entities match the schema (`ddl-auto: validate` in
 * the test `application.yaml`). Before each test, [clearDatabase] runs a data reset through [DataResetService].
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
