package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import de.seuhd.campuscoffee.tests.UserRequests
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * System tests for listing, reading, and creating users. The tests send their requests through [UserRequests]. A
 * request that creates a user has the admin `jane_doe` as the acting user, unless a test sets another acting user or
 * leaves the header `X-Acting-User` out.
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
    fun `creating a user returns 201 Created with the URL of the new user`() {
        val response = UserRequests.create(newUser())

        assertThat(response.status).isEqualTo(201)
        val created = response.body
        assertThat(created.loginName).isEqualTo("new_user")
        assertThat(created.emailAddress).isEqualTo("new.user@se.uni-heidelberg.de")
        assertThat(created.firstName).isEqualTo("New")
        assertThat(created.lastName).isEqualTo("User")
        assertThat(created.createdAt).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(1, ChronoUnit.MINUTES))
        val location = requireNotNull(response.headers.location)
        assertThat(UserRequests.retrieveAt(location).body).usingRecursiveComparison().isEqualTo(created)
    }

    @Test
    fun `creating a user creates a consumption with a count of zero`() {
        val created = UserRequests.create(newUser()).body

        assertThat(ConsumptionRequests.retrieveConsumption(created.persistedId).body.count).isEqualTo(0)
    }

    @Test
    fun `getting a user by id returns 200 OK with the user`() {
        val lisa = fixtureUser("lisa_lee")

        val response = UserRequests.retrieveById(lisa.persistedId)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).usingRecursiveComparison().isEqualTo(lisa)
    }

    @Test
    fun `creating a user with a query string returns a Location header without the query string`() {
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
    fun `creating a user with a taken login name returns 409 Conflict`() {
        val response = UserRequests.create(newUser(loginName = "jane_doe"))

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("DuplicationException")
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
    }

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `creating a user logs neither a taken email address nor the login name of an unknown acting user`(
        output: CapturedOutput
    ) {
        UserRequests.create(newUser(emailAddress = "jane.doe@se.uni-heidelberg.de")).requireStatus(409)
        UserRequests.create(newUser(), actingUser = "nobody").requireStatus(401)

        assertThat(output.all).doesNotContain("jane.doe@se.uni-heidelberg.de", "nobody")
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 255])
    fun `creating a user with a login name of 1 or 255 characters returns 201 Created`(length: Int) {
        val response = UserRequests.create(newUser(loginName = "a".repeat(length)))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.loginName).hasSize(length)
    }

    @Test
    fun `creating a user with a login name of 256 characters returns 400 Bad Request`() {
        val response = UserRequests.create(newUser(loginName = "a".repeat(256)))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with capital letters in the email address returns 201 Created with a lowercase address`() {
        val response = UserRequests.create(newUser(emailAddress = "New.User@SE.Uni-Heidelberg.DE"))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.emailAddress).isEqualTo("new.user@se.uni-heidelberg.de")
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
    fun `creating a user with a space in the login name returns 400 Bad Request without the login name`() {
        val response = UserRequests.create(newUser(loginName = "jane doe"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(response.error.message).contains("loginName").doesNotContain("jane doe")
    }

    @Test
    fun `creating a user with an id in the body returns 400 Bad Request`() {
        val body = newUser() + ("id" to UUID.randomUUID().toString())

        val response = UserRequests.create(body)

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user without the acting user header returns 401 Unauthorized`() {
        val response = UserRequests.create(newUser(), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user as a user who is not an admin returns 403 Forbidden`() {
        val response = UserRequests.create(newUser(), actingUser = "maxmustermann")

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an unknown acting user returns 401 Unauthorized`() {
        val response = UserRequests.create(newUser(), actingUser = "nobody")

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.error.message).doesNotContain("nobody")
        assertThat(response.headers.getFirst("WWW-Authenticate")).isEqualTo(CHALLENGE)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with an acting user in other letter case returns 401 Unauthorized`() {
        val response = UserRequests.create(newUser(), actingUser = "JANE_DOE")

        assertThat(response.status).isEqualTo(401)
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
    fun `creating a user as an admin whom another admin created returns 201 Created`() {
        UserRequests.create(newUser(loginName = "second_admin") + ("role" to "ADMIN")).requireStatus(201)

        val response =
            UserRequests.create(
                newUser(loginName = "third_user", emailAddress = "third.user@se.uni-heidelberg.de"),
                actingUser = "second_admin"
            )

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body.role).isEqualTo(Role.USER)
    }

    @Test
    fun `creating a user with an unknown role returns 400 Bad Request`() {
        val response = UserRequests.create(newUser() + ("role" to "OWNER"))

        assertThat(response.status).isEqualTo(400)
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a number as the role returns 400 Bad Request`() {
        val response = UserRequests.create(newUser() + ("role" to 1))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a number as the login name returns 400 Bad Request`() {
        val response = UserRequests.create(newUser() + ("loginName" to 12345))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(userService.getAll()).hasSize(fixtureUsers.size)
    }

    @Test
    fun `creating a user with a boolean as the first name returns 400 Bad Request`() {
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
