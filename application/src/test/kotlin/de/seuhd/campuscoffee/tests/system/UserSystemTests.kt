package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import de.seuhd.campuscoffee.tests.UserRequests
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * System tests for listing, reading, and creating users. The tests send their requests through [UserRequests], which
 * sends a request that creates a user with the login name of the admin `jane_doe` in the header `X-Acting-User` unless
 * a test leaves the header out or sets another login name.
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

    @Test
    fun `listing users returns 200 OK with the fixture users ordered by login name`() {
        val response = UserRequests.retrieveAll()

        assertThat(response.status).isEqualTo(200)
        val loginNames = response.body.map { it.loginName }
        assertThat(loginNames).containsExactly("jane_doe", "lisa_lee", "maxmustermann", "olivia_lee", "student2023")
    }

    @Test
    fun `listing users returns the content type application json with the charset UTF-8`() {
        val response = UserRequests.retrieveAll()

        assertThat(response.headers.contentType)
            .isEqualTo(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
    }

    @Test
    fun `creating a user returns 201 Created with a Location header and a coffee count of zero`() {
        val response = UserRequests.create(newUser())

        assertThat(response.status).isEqualTo(201)
        val created = response.body
        assertThat(created.loginName).isEqualTo("new_user")
        assertThat(created.createdAt).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(1, ChronoUnit.MINUTES))
        assertThat(response.headers.location?.path).isEqualTo("/api/users/${created.persistedId}")
        val consumption = ConsumptionRequests.retrieveConsumption(created.persistedId)
        assertThat(consumption.status).isEqualTo(200)
        assertThat(consumption.body.count).isEqualTo(0)
    }

    @Test
    fun `getting a user by id returns 200 OK with the user`() {
        val lisa = fixtureUser("lisa_lee")

        val response = UserRequests.retrieveById(lisa.persistedId)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.loginName).isEqualTo("lisa_lee")
        assertThat(response.body.emailAddress).isEqualTo(lisa.emailAddress)
    }

    @Test
    fun `creating a user with a query string returns 201 Created with a Location header without the query string`() {
        val response = UserRequests.create(newUser(), queryString = "source=test")

        assertThat(response.status).isEqualTo(201)
        assertThat(response.headers.location?.query).isNull()
        assertThat(response.headers.location?.path).isEqualTo("/api/users/${response.body.persistedId}")
    }

    @Test
    fun `getting a user with an id that is not a UUID returns 400 Bad Request`() {
        val response = UserRequests.retrieveById("not-a-uuid")

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `getting an unknown user returns 404 Not Found`() {
        val response = UserRequests.retrieveById(UUID.randomUUID())

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `creating a user with a taken login name returns 409 Conflict`() {
        val response = UserRequests.create(newUser(loginName = "jane_doe"))

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("DuplicationException")
        assertThat(response.error.message).isEqualTo("User with login name 'jane_doe' already exists.")
    }

    @Test
    fun `creating a user with a taken login name in other letter case returns 201 Created`() {
        val response = UserRequests.create(newUser(loginName = "Jane_Doe"))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.loginName).isEqualTo("Jane_Doe")
    }

    @Test
    fun `creating a user with a taken email address in other letter case returns 409 Conflict`() {
        val response = UserRequests.create(newUser(emailAddress = "Jane.Doe@SE.Uni-Heidelberg.de"))

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("DuplicationException")
        assertThat(response.error.message)
            .isEqualTo("User with email address 'jane.doe@se.uni-heidelberg.de' already exists.")
    }

    @Test
    fun `creating a user with a blank login name returns 400 Bad Request`() {
        val response = UserRequests.create(newUser(loginName = ""))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user without an email address returns 400 Bad Request`() {
        val response = UserRequests.create(newUser() - "emailAddress")

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user with a blank first name returns 400 Bad Request`() {
        val response = UserRequests.create(newUser(firstName = "   "))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `creating a user with a NUL in the first name returns 400 Bad Request with ValidationException`() {
        val response = UserRequests.create(newUser(firstName = "Ne\u0000w"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
    }

    @Test
    fun `creating a user with a quoted email address with a space returns 400 Bad Request with ValidationException`() {
        val response = UserRequests.create(newUser(emailAddress = "\"jane doe\"@se.de"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
    }

    @Test
    fun `creating a user with a space in the login name returns 400 Bad Request that names the field`() {
        val response = UserRequests.create(newUser(loginName = "jane doe"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.message)
            .isEqualTo("loginName must contain only ASCII letters, digits, and underscores")
    }

    @Test
    fun `creating a user with an id in the body returns 400 Bad Request`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val response = UserRequests.create(body)

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
    }

    @Test
    fun `creating a user without the acting user header returns 401 Unauthorized and creates no user`() {
        val response = UserRequests.create(newUser(), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.error.message).isEqualTo("The header X-Acting-User is missing.")
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user as a user who is not an admin returns 403 Forbidden and creates no user`() {
        val response = UserRequests.create(newUser(), actingUser = "maxmustermann")

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
        assertThat(response.error.message).isEqualTo("Only an admin may create a user.")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an unknown acting user returns 401 Unauthorized and creates no user`() {
        val response = UserRequests.create(newUser(), actingUser = "nobody")

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.error.message).isEqualTo("No user has the login name in X-Acting-User.")
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an invalid body and without the acting user header returns 401 Unauthorized`() {
        val response = UserRequests.create(newUser(loginName = " "), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
    }

    @Test
    fun `listing users with an unknown acting user returns 401 Unauthorized`() {
        val response = UserRequests.retrieveAll(actingUser = "nobody")

        assertThat(response.status).isEqualTo(401)
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
    }

    @Test
    fun `creating a user without the acting user header and with an id returns 401 Unauthorized`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val response = UserRequests.create(body, actingUser = null)

        assertThat(response.status).isEqualTo(401)
    }

    @Test
    fun `creating a user without a role returns 201 Created with the role USER`() {
        val response = UserRequests.create(newUser())

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.role).isEqualTo(Role.USER)
    }

    @Test
    fun `creating a user with the role ADMIN as an admin returns 201 Created with the role ADMIN`() {
        val response = UserRequests.create(newUser() + ("role" to "ADMIN"))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `creating a user with an unknown role returns 400 Bad Request and creates no user`() {
        val response = UserRequests.create(newUser() + ("role" to "OWNER"))

        assertThat(response.status).isEqualTo(400)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a number as the role returns 400 Bad Request and creates no user`() {
        val response = UserRequests.create(newUser() + ("role" to 1))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a number as the login name returns 400 Bad Request and creates no user`() {
        val response = UserRequests.create(newUser() + ("loginName" to 12345))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a boolean as the first name returns 400 Bad Request and creates no user`() {
        val response = UserRequests.create(newUser() + ("firstName" to true))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    private companion object {
        /** The challenge of Coffee Kitty in the header WWW-Authenticate, which RFC 9110 requires in a 401. */
        const val CHALLENGE = "X-Acting-User realm=\"Coffee Kitty\""
    }
}
