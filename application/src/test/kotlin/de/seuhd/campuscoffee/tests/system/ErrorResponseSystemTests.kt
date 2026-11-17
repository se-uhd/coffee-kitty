package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.tests.Response
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.UserRequests
import de.seuhd.campuscoffee.tests.toResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * System tests for the [ErrorResponse] body. They check every field for a domain exception and the error code for an
 * error that Spring MVC reports. That error code is the reason phrase of the status without spaces, such as
 * `NotFound`.
 */
class ErrorResponseSystemTests : AbstractSystemTest() {
    /**
     * Sends a GET request to [path]. The tests for an unknown path and for a path that the firewall rejects build
     * their request here, because the path itself is under test and no function of [UserRequests] sends it.
     */
    private fun get(path: String): Response<ErrorResponse> =
        client()
            .get()
            .uri(path)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    @Test
    fun `getting an unknown user returns 404 Not Found with every field of the error response`() {
        val response = UserRequests.retrieveById(UUID.fromString("00000000-0000-0000-0000-000000000001"))

        assertThat(response.status).isEqualTo(404)
        val error = response.error
        assertThat(error.errorCode).isEqualTo("NotFoundException")
        assertThat(error.message).isEqualTo("User with ID 00000000-0000-0000-0000-000000000001 does not exist.")
        assertThat(error.statusCode).isEqualTo(404)
        assertThat(error.statusMessage).isEqualTo("Not Found")
        assertThat(error.path).isEqualTo("/api/users/00000000-0000-0000-0000-000000000001")
        assertThat(error.timestamp).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), byLessThanOneMinute)
    }

    @Test
    fun `requesting an unknown path returns 404 Not Found with the error code NotFound`() {
        val response = get("/api/unknown")

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFound")
        assertThat(response.error.message).isEqualTo("Not Found")
    }

    @Test
    fun `requesting a path with a semicolon returns 400 Bad Request with a message that does not quote the path`() {
        val response = get("/api/users;x=1")

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(response.error.message).isEqualTo("The request could not be read.")
        assertThat(response.error.path).isEqualTo("/api/users;x=1")
    }

    private companion object {
        /** The tolerance for comparing the UTC timestamp of an error response with the current UTC time. */
        val byLessThanOneMinute = within(1, ChronoUnit.MINUTES)
    }
}
