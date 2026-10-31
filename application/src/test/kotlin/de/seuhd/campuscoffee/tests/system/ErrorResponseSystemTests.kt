package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * System tests for the [ErrorResponse] body: its fields for a domain exception and the neutral error code of an error
 * that Spring MVC itself reports.
 */
class ErrorResponseSystemTests : AbstractSystemTest() {
    private fun get(path: String) =
        client()
            .get()
            .uri(path)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .returnResult<ErrorResponse>()

    @Test
    fun `getting an unknown user returns 404 Not Found with every field of the error response`() {
        val path = "/api/users/00000000-0000-0000-0000-000000000001"

        val result = get(path)

        assertThat(result.status.value()).isEqualTo(404)
        val error = result.responseBody!!
        assertThat(error.errorCode).isEqualTo("NotFoundException")
        assertThat(error.message).isEqualTo("User with ID 00000000-0000-0000-0000-000000000001 does not exist.")
        assertThat(error.statusCode).isEqualTo(404)
        assertThat(error.statusMessage).isEqualTo("Not Found")
        assertThat(error.path).isEqualTo(path)
        assertThat(error.timestamp).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), byLessThanOneMinute)
    }

    @Test
    fun `requesting an unknown path returns 404 Not Found with the error code NotFound`() {
        val result = get("/api/unknown")

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFound")
        assertThat(result.responseBody!!.message).isEqualTo("Not Found")
    }

    private companion object {
        /** The tolerance for comparing the UTC timestamp of an error response with the current UTC time. */
        val byLessThanOneMinute = within(1, ChronoUnit.MINUTES)
    }
}
