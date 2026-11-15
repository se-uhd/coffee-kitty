package de.seuhd.campuscoffee.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.web.firewall.RequestRejectedException
import tools.jackson.databind.json.JsonMapper

/**
 * Unit tests for the handler of rejected requests in [SecurityConfig]. Because the test client of the system tests
 * turns `//` into `/`, this test passes such a path to the handler directly.
 */
class SecurityConfigTest {
    @Test
    fun `a rejected request gets 400 Bad Request with an error response without the message of the exception`() {
        val response = MockHttpServletResponse()

        SecurityConfig()
            .requestRejectedHandler(JsonMapper.builder().build())
            .handle(MockHttpServletRequest("GET", "/api//users"), response, RequestRejectedException("rejected //"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.contentAsString)
            .contains("\"errorCode\":\"BadRequest\"", "\"message\":\"The request could not be read.\"")
            .doesNotContain("rejected")
    }
}
