package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

/**
 * System tests for the OpenAPI definition. The dev profile serves it at `/api/api-docs`. These tests turn it on
 * without that profile, so it is at the springdoc default path `/v3/api-docs`.
 */
@TestPropertySource(properties = ["springdoc.api-docs.enabled=true"])
class OpenApiSystemTests : AbstractSystemTest() {
    @Test
    fun `the OpenAPI definition marks the server-assigned fields of a user as read-only and the others as writable`() {
        val userProperties = "$.components.schemas.UserDto.properties"

        client()
            .get()
            .uri("/v3/api-docs")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$userProperties.id.readOnly")
            .isEqualTo(true)
            .jsonPath("$userProperties.createdAt.readOnly")
            .isEqualTo(true)
            .jsonPath("$userProperties.updatedAt.readOnly")
            .isEqualTo(true)
            .jsonPath("$userProperties.loginName.readOnly")
            .doesNotExist()
            .jsonPath("$userProperties.emailAddress.readOnly")
            .doesNotExist()
            .jsonPath("$userProperties.firstName.readOnly")
            .doesNotExist()
            .jsonPath("$userProperties.lastName.readOnly")
            .doesNotExist()
    }
}
