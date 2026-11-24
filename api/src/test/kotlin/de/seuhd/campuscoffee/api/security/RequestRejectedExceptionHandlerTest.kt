package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.api.exceptions.GlobalExceptionHandler
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.security.web.firewall.RequestRejectedException
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper

/**
 * The unit test for [RequestRejectedExceptionHandler]. The firewall rejects some requests only while Spring MVC reads
 * them, such as a request with a control character in a header. In the test, a route throws the
 * [RequestRejectedException] of the firewall. MockMvc runs the route with the handler and [GlobalExceptionHandler], so
 * that the test fails when the handler does not take precedence.
 */
class RequestRejectedExceptionHandlerTest {
    @RestController
    private class ExampleController {
        @GetMapping("/api/example")
        fun example(): String = throw RequestRejectedException("header value with a bell character")
    }

    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(ExampleController())
            .setControllerAdvice(
                GlobalExceptionHandler(),
                RequestRejectedExceptionHandler(JsonMapper.builder().build())
            ).build()

    @Test
    fun `a request that the firewall rejects returns 400 Bad Request without the message of the exception`() {
        mockMvc.get("/api/example").andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("BadRequest") }
            content { string(not(containsString("bell"))) }
        }
    }
}
