package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.api.exceptions.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper

/**
 * The unit test for [AuthenticationExceptionHandler] on an endpoint without a rule in `SecurityConfig`. MockMvc runs
 * the endpoint with the handler and [GlobalExceptionHandler], so that the test fails when the handler does not take
 * precedence.
 */
class AuthenticationExceptionHandlerTest {
    @RestController
    private class ExampleController {
        @GetMapping("/api/example")
        fun example(): String = CurrentUserProvider().currentUser().loginName
    }

    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(ExampleController())
            .setControllerAdvice(
                GlobalExceptionHandler(),
                AuthenticationExceptionHandler(JsonAuthenticationEntryPoint(JsonMapper.builder().build()))
            ).build()

    @Test
    fun `an endpoint that needs an acting user returns 401 Unauthorized for a request without an acting user`() {
        mockMvc.get("/api/example").andExpect {
            status { isUnauthorized() }
            header { string("WWW-Authenticate", "X-Acting-User realm=\"Coffee Kitty\"") }
            jsonPath("$.errorCode") { value("Unauthorized") }
            jsonPath("$.path") { value("/api/example") }
        }
    }
}
