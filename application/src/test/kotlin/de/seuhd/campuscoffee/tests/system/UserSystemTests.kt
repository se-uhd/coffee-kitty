package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.util.UUID

/**
 * System tests for the user endpoints under `/api/users`, which list, get, and create users.
 */
class UserSystemTests : AbstractSystemTest() {
    /** The request body of a new user with the given login name. */
    private fun newUser(loginName: String = "new_user"): Map<String, Any> =
        mapOf(
            "loginName" to loginName,
            "emailAddress" to "new.user@se.uni-heidelberg.de",
            "firstName" to "New",
            "lastName" to "User"
        )

    private fun createUser(body: Map<String, Any>) =
        client()
            .post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()

    private fun getUser(id: UUID) =
        client()
            .get()
            .uri("/api/users/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()

    @Test
    fun `listing users returns 200 with the fixture users ordered by login name`() {
        val result =
            client()
                .get()
                .uri("/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<Array<UserDto>>()

        assertThat(result.status.value()).isEqualTo(200)
        val loginNames = result.responseBody!!.map { it.loginName }
        assertThat(loginNames).containsExactly("jane_doe", "lisa_lee", "maxmustermann", "olivia_lee", "student2023")
    }

    @Test
    fun `creating a user returns 201 with a Location header and a coffee count of zero`() {
        val result = createUser(newUser()).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        val created = result.responseBody!!
        assertThat(created.loginName).isEqualTo("new_user")
        assertThat(result.responseHeaders.location?.path).isEqualTo("/api/users/${created.persistedId}")
        val consumption =
            client()
                .get()
                .uri("/api/users/{id}/consumption", created.persistedId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<ConsumptionDto>()
        assertThat(consumption.status.value()).isEqualTo(200)
        assertThat(consumption.responseBody!!.count).isEqualTo(0)
    }

    @Test
    fun `getting a user by id returns 200 with the user`() {
        val lisa = seededUser("lisa_lee")

        val result = getUser(lisa.persistedId).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(200)
        assertThat(result.responseBody!!.loginName).isEqualTo("lisa_lee")
        assertThat(result.responseBody!!.emailAddress).isEqualTo(lisa.emailAddress)
    }

    @Test
    fun `getting an unknown user returns 404 Not Found`() {
        val result = getUser(UUID.randomUUID()).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `creating a user with a taken login name returns 409 Conflict`() {
        val result = createUser(newUser(loginName = "jane_doe")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(409)
        assertThat(result.responseBody!!.errorCode).isEqualTo("DuplicationException")
        assertThat(result.responseBody!!.message).isEqualTo("User with login name 'jane_doe' already exists.")
    }

    @Test
    fun `creating a user with a blank login name returns 400 Bad Request`() {
        val result = createUser(newUser(loginName = "")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user with an id in the body returns 400 Bad Request`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val result = createUser(body).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
    }
}
