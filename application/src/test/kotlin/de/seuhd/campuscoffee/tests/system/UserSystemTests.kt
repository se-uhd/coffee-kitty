package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * System tests for the user endpoints under `/api/users`, which list, get, and create users.
 */
class UserSystemTests : AbstractSystemTest() {
    /** The request body of a new user with the given login name, email address, and first name. */
    private fun newUser(
        loginName: String = "new_user",
        emailAddress: String = "new.user@se.uni-heidelberg.de",
        firstName: String = "New"
    ): Map<String, Any> =
        mapOf(
            "loginName" to loginName,
            "emailAddress" to emailAddress,
            "firstName" to firstName,
            "lastName" to "User"
        )

    private fun createUser(
        body: Map<String, Any>,
        uri: String = "/api/users"
    ) = client()
        .post()
        .uri(uri)
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
    fun `listing users returns 200 OK with the fixture users ordered by login name`() {
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
    fun `listing users returns the content type application json with the charset UTF-8`() {
        val result =
            client()
                .get()
                .uri("/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<ByteArray>()

        assertThat(result.responseHeaders.contentType)
            .isEqualTo(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
    }

    @Test
    fun `creating a user returns 201 Created with a Location header and a coffee count of zero`() {
        val result = createUser(newUser()).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        val created = result.responseBody!!
        assertThat(created.loginName).isEqualTo("new_user")
        assertThat(created.createdAt).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(1, ChronoUnit.MINUTES))
        assertThat(result.responseHeaders.location?.path).isEqualTo("/api/users/${created.persistedId}")
        val consumption =
            client()
                .get()
                .uri("/api/users/{userId}/consumption", created.persistedId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<ConsumptionDto>()
        assertThat(consumption.status.value()).isEqualTo(200)
        assertThat(consumption.responseBody!!.count).isEqualTo(0)
    }

    @Test
    fun `getting a user by id returns 200 OK with the user`() {
        val lisa = fixtureUser("lisa_lee")

        val result = getUser(lisa.persistedId).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(200)
        assertThat(result.responseBody!!.loginName).isEqualTo("lisa_lee")
        assertThat(result.responseBody!!.emailAddress).isEqualTo(lisa.emailAddress)
    }

    @Test
    fun `creating a user with a query string returns 201 Created with a Location header without the query string`() {
        val result = createUser(newUser(), uri = "/api/users?source=test").returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        assertThat(result.responseHeaders.location?.query).isNull()
        assertThat(result.responseHeaders.location?.path).isEqualTo("/api/users/${result.responseBody!!.persistedId}")
    }

    @Test
    fun `getting a user with an id that is not a UUID returns 400 Bad Request`() {
        val result =
            client()
                .get()
                .uri("/api/users/not-a-uuid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
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
    fun `creating a user with a taken login name in other letter case returns 201 Created`() {
        val result = createUser(newUser(loginName = "Jane_Doe")).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        assertThat(result.responseBody!!.loginName).isEqualTo("Jane_Doe")
    }

    @Test
    fun `creating a user with a taken email address in other letter case returns 409 Conflict`() {
        val result =
            createUser(newUser(emailAddress = "Jane.Doe@SE.Uni-Heidelberg.de")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(409)
        assertThat(result.responseBody!!.errorCode).isEqualTo("DuplicationException")
        assertThat(result.responseBody!!.message)
            .isEqualTo("User with email address 'jane.doe@se.uni-heidelberg.de' already exists.")
    }

    @Test
    fun `creating a user with a blank login name returns 400 Bad Request`() {
        val result = createUser(newUser(loginName = "")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user without an email address returns 400 Bad Request`() {
        val result = createUser(newUser() - "emailAddress").returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user with a blank first name returns 400 Bad Request`() {
        val result = createUser(newUser(firstName = "   ")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user with a NUL in the first name returns 400 Bad Request with ValidationException`() {
        val result = createUser(newUser(firstName = "Ne\u0000w")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
    }

    @Test
    fun `creating a user with a quoted email address with a space returns 400 Bad Request with ValidationException`() {
        val result = createUser(newUser(emailAddress = "\"jane doe\"@se.de")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
    }

    @Test
    fun `creating a user with a space in the login name returns 400 Bad Request that names the field`() {
        val result = createUser(newUser(loginName = "jane doe")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.message)
            .isEqualTo("loginName must contain only ASCII letters, digits, and underscores")
    }

    @Test
    fun `creating a user with an id in the body returns 400 Bad Request`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val result = createUser(body).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
    }
}
