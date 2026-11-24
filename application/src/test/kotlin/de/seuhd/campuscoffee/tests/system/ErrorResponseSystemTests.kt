package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ErrorJson
import de.seuhd.campuscoffee.tests.Response
import de.seuhd.campuscoffee.tests.SystemTestUtils.ACTING_USER_HEADER
import de.seuhd.campuscoffee.tests.SystemTestUtils.ADMIN_LOGIN_NAME
import de.seuhd.campuscoffee.tests.UserRequests
import de.seuhd.campuscoffee.tests.parseUtc
import de.seuhd.campuscoffee.tests.toResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * The system tests for the [ErrorJson] body. For a domain exception, the error code is the class name of the exception.
 * For an error that Spring MVC or the firewall reports, the error code is the reason phrase of the status without
 * spaces, such as `NotFound`.
 */
class ErrorResponseSystemTests : AbstractSystemTest() {
    /**
     * Sends a GET request to [path]. A test calls this function when the path itself is under test. No function of
     * [UserRequests] sends a request to an unknown path or to a path that the firewall rejects.
     */
    private fun get(path: String): Response<ErrorJson> =
        client
            .get()
            .uri(path)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    @Test
    fun `getting an unknown user returns 404 Not Found with the error code, the status, the path, and the timestamp`() {
        val response = userRequests.retrieveById(UUID.fromString("00000000-0000-0000-0000-000000000001"))

        assertThat(response.status).isEqualTo(404)
        val error = response.error
        assertThat(error.errorCode).isEqualTo("NotFoundException")
        assertThat(error.statusCode).isEqualTo(404)
        assertThat(error.statusMessage).isEqualTo("Not Found")
        assertThat(error.path).isEqualTo("/api/users/00000000-0000-0000-0000-000000000001")
        assertThat(parseUtc(error.timestamp)).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), byLessThanOneMinute)
    }

    @Test
    fun `requesting an unknown path returns 404 Not Found with the error code NotFound`() {
        val response = get("/api/unknown")

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFound")
    }

    @Test
    fun `an error response has the content type application json with the charset UTF-8`() {
        val response = get("/api/unknown")

        assertThat(response.headers.contentType)
            .isEqualTo(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
    }

    @Test
    fun `posting a text as the delta returns 400 Bad Request without the text in the message`() {
        val userId = userFixture("maxmustermann").persistedId

        val response = consumptionRequests.applyDelta(userId, mapOf("delta" to "one coffee"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(response.error.message).doesNotContain("one coffee")
    }

    @Test
    fun `requesting a path with a semicolon returns 400 Bad Request without the path in the message`() {
        val response = get("/api/users;x=1")

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(response.error.path).isEqualTo("/api/users;x=1")
        assertThat(response.error.message).doesNotContain(";")
    }

    @Test
    fun `creating a user with a YAML request body returns 415 Unsupported Media Type`() {
        val usersBefore = userRequests.retrieveAll().body

        val response: Response<ErrorJson> =
            client
                .post()
                .uri("/api/users")
                .contentType(YAML)
                .accept(MediaType.APPLICATION_JSON)
                .header(ACTING_USER_HEADER, ADMIN_LOGIN_NAME)
                .body(
                    """
                    loginName: yaml_user
                    emailAddress: yaml.user@example.com
                    firstName: Yaml
                    lastName: User
                    """.trimIndent()
                ).exchange()
                .toResponse()

        assertThat(response.status).isEqualTo(415)
        assertThat(response.error.errorCode).isEqualTo("UnsupportedMediaType")
        assertThat(userRequests.retrieveAll().body).isEqualTo(usersBefore)
    }

    @Test
    fun `creating a user with a wildcard Content-Type returns 415 Unsupported Media Type`() {
        val usersBefore = userRequests.retrieveAll().body

        for (contentType in listOf("*/*", "application/*", "application/*+json")) {
            val response: Response<ErrorJson> =
                client
                    .post()
                    .uri("/api/users")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(ACTING_USER_HEADER, ADMIN_LOGIN_NAME)
                    .body(
                        """{"loginName":"wildcard_user","emailAddress":"w@example.com","firstName":"W","lastName":"U"}"""
                    ).exchange()
                    .toResponse()

            assertThat(response.status).`as`(contentType).isEqualTo(415)
            assertThat(response.error.errorCode).`as`(contentType).isEqualTo("UnsupportedMediaType")
        }
        assertThat(userRequests.retrieveAll().body).isEqualTo(usersBefore)
    }

    @Test
    fun `getting the users with the Accept header for YAML returns 406 Not Acceptable`() {
        val response: Response<ErrorJson> =
            client
                .get()
                .uri("/api/users")
                .accept(YAML)
                .exchange()
                .toResponse()

        assertThat(response.status).isEqualTo(406)
        assertThat(response.error.errorCode).isEqualTo("NotAcceptable")
    }

    private companion object {
        /** The media type of a YAML body, which the API neither reads nor writes. */
        val YAML: MediaType = MediaType.parseMediaType("application/yaml")

        /** The tolerance for comparing the UTC timestamp of an error response with the current UTC time. */
        val byLessThanOneMinute = within(1, ChronoUnit.MINUTES)
    }
}
