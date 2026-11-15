package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.domain.model.Role
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
 * System tests for listing, reading, and creating users through `/api/users`. The requests that create a user have the
 * login name of the admin `jane_doe` in the header `X-Acting-User` unless a test leaves the header out or sets another
 * login name.
 */
class UserSystemTests : AbstractSystemTest() {
    /**
     * Returns the request body for a new user with [loginName], [emailAddress], [firstName], and the last name `User`.
     */
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
        uri: String = "/api/users",
        actingUser: String? = ADMIN_LOGIN_NAME
    ) = client()
        .post()
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .headers { headers -> actingUser?.let { headers.set(CurrentUserProvider.ACTING_USER_HEADER, it) } }
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

    @Test
    fun `creating a user without the acting user header returns 401 Unauthorized and creates no user`() {
        val result = createUser(newUser(), actingUser = null).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(401)
        assertThat(result.responseBody!!.errorCode).isEqualTo("Unauthorized")
        assertThat(result.responseBody!!.message).isEqualTo("The header X-Acting-User is missing.")
        assertThat(result.responseHeaders.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user as a user who is not an admin returns 403 Forbidden and creates no user`() {
        val result = createUser(newUser(), actingUser = "maxmustermann").returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(403)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ForbiddenException")
        assertThat(result.responseBody!!.message).isEqualTo("Only an admin may create a user.")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an unknown acting user returns 401 Unauthorized and creates no user`() {
        val result = createUser(newUser(), actingUser = "nobody").returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(401)
        assertThat(result.responseBody!!.errorCode).isEqualTo("Unauthorized")
        assertThat(result.responseBody!!.message).isEqualTo("No user has the login name in X-Acting-User.")
        assertThat(result.responseHeaders.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an invalid body and without the acting user header returns 401 Unauthorized`() {
        val result = createUser(newUser(loginName = " "), actingUser = null).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(401)
        assertThat(result.responseHeaders.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
    }

    @Test
    fun `listing users with an unknown acting user returns 401 Unauthorized`() {
        val result =
            client()
                .get()
                .uri("/api/users")
                .header(CurrentUserProvider.ACTING_USER_HEADER, "nobody")
                .exchange()
                .returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(401)
        assertThat(result.responseHeaders.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
    }

    @Test
    fun `creating a user without the acting user header and with an id returns 401 Unauthorized`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val result = createUser(body, actingUser = null).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(401)
    }

    @Test
    fun `creating a user without a role returns 201 Created with the role USER`() {
        val result = createUser(newUser()).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        assertThat(result.responseBody!!.role).isEqualTo(Role.USER)
    }

    @Test
    fun `creating a user with the role ADMIN as an admin returns 201 Created with the role ADMIN`() {
        val result = createUser(newUser() + ("role" to "ADMIN")).returnResult<UserDto>()

        assertThat(result.status.value()).isEqualTo(201)
        assertThat(result.responseBody!!.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `creating a user with an unknown role returns 400 Bad Request and creates no user`() {
        val result = createUser(newUser() + ("role" to "OWNER")).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    private companion object {
        /** The fixture user with the role ADMIN, who may create users. */
        const val ADMIN_LOGIN_NAME = "jane_doe"

        /** The challenge of Coffee Kitty in the header WWW-Authenticate, which RFC 9110 requires in a 401. */
        const val CHALLENGE = "X-Acting-User realm=\"Coffee Kitty\""
    }
}
