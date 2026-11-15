package de.seuhd.campuscoffee.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.web.firewall.RequestRejectedException
import tools.jackson.databind.json.JsonMapper

/**
 * Unit test for [RequestRejectedExceptionHandler], which responds to a request that the firewall of Spring Security
 * rejects while Spring MVC reads it, such as one with a control character in a header. HTTP clients refuse to send such
 * a header, which rules out a system test.
 */
class RequestRejectedExceptionHandlerTest {
    @Test
    fun `handleRequestRejected turns a RequestRejectedException into 400 Bad Request without the rejected value`() {
        val response = MockHttpServletResponse()

        RequestRejectedExceptionHandler(JsonMapper.builder().build()).handleRequestRejected(
            RequestRejectedException("header value with a bell character"),
            MockHttpServletRequest("POST", "/api/users"),
            response
        )

        assertThat(response.status).isEqualTo(400)
        assertThat(response.contentAsString)
            .contains("\"errorCode\":\"BadRequest\"", "\"message\":\"The request could not be read.\"")
            .doesNotContain("bell")
    }
}
