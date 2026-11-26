package de.seuhd.campuscoffee.api.exceptions

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The unit test for the responses of [GlobalExceptionHandler] that no endpoint of Coffee Kitty causes on purpose, such
 * as an `IllegalArgumentException` of an endpoint that reads no body. MockMvc runs the endpoint of the test with the
 * handler. `ErrorResponseSystemTests` checks the 415 for a body with a wildcard `Content-Type`.
 */
class GlobalExceptionHandlerTest {
    @RestController
    private class ExampleController {
        @GetMapping("/api/failing")
        fun failing(): String = throw IllegalArgumentException("Required value was null.")
    }

    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(ExampleController())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

    @Test
    fun `an IllegalArgumentException of an endpoint on a request with a wildcard Content-Type returns 500`() {
        mockMvc
            .get("/api/failing") {
                header(HttpHeaders.CONTENT_TYPE, "*/*")
            }.andExpect {
                status { isInternalServerError() }
                jsonPath("$.errorCode") { value("InternalServerError") }
            }
    }
}
