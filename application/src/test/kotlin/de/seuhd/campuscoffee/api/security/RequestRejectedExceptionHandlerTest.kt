package de.seuhd.campuscoffee.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.web.firewall.RequestRejectedException
import tools.jackson.databind.json.JsonMapper

/**
 * Unit test for [RequestRejectedExceptionHandler], which answers a request that the firewall of Spring Security rejects
 * while a controller reads it, such as a header value with a control character. HTTP clients refuse to send such a
 * header, so a system test cannot send one.
 */
class RequestRejectedExceptionHandlerTest {
    @Test
    fun `a request that the firewall rejects in a controller gets 400 Bad Request without the rejected value`() {
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
