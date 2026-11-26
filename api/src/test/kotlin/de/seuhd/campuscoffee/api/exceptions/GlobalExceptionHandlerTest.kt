package de.seuhd.campuscoffee.api.exceptions

import org.apache.catalina.connector.ClientAbortException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException

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

        @GetMapping("/api/disconnected")
        fun disconnected(): String = throw ClientAbortException(IOException("Broken pipe"))
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

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `an exception of a client that disconnected gets no error body and no log at ERROR`(output: CapturedOutput) {
        mockMvc.get("/api/disconnected").andExpect {
            content { string("") }
        }

        assertThat(output.all).doesNotContain("ERROR", "Unexpected error")
    }
}
