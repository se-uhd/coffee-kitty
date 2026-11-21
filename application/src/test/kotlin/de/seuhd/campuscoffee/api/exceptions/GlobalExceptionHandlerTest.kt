package de.seuhd.campuscoffee.api.exceptions

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.stream.Stream

/**
 * Unit tests for [GlobalExceptionHandler]. Each test passes an exception and a request to the exception handler and
 * checks the returned error response. The tests need neither a Spring context nor a controller.
 * `ErrorResponseSystemTests` checks that Spring MVC passes the exception of a controller to the exception handler.
 */
class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    private val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/users"))

    @ParameterizedTest
    @MethodSource("domainExceptionsAndStatuses")
    fun `handleMappedException maps each domain exception to its status`(
        exception: Exception,
        status: HttpStatus
    ) {
        val response = handler.handleMappedException(exception, request)

        assertThat(response.statusCode).isEqualTo(status)
    }

    @Test
    fun `handleMappedException uses the class name of the domain exception as the error code`() {
        val response = handler.handleMappedException(ConflictException("There is no coffee to undo."), request)

        assertThat(response.body?.errorCode).isEqualTo("ConflictException")
    }

    @Test
    fun `handleMappedException returns the message of the domain exception and the path of the request`() {
        val response = handler.handleMappedException(ConflictException("There is no coffee to undo."), request)

        assertThat(response.body?.message).isEqualTo("There is no coffee to undo.")
        assertThat(response.body?.path).isEqualTo("/api/users")
    }

    @Test
    fun `handleMappedException for an exception that is not a domain exception returns 500 Internal Server Error`() {
        val response = handler.handleMappedException(IllegalStateException("The check failed."), request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `handleGenericException returns 500 with a fixed message in place of the message of the exception`() {
        val response = handler.handleGenericException(IllegalStateException("The check failed."), request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.errorCode).isEqualTo("InternalServerError")
        assertThat(response.body?.message).isEqualTo("An unexpected error occurred.")
    }

    @Test
    fun `an error response has the content type application json with the charset UTF-8`() {
        val response = handler.handleMappedException(ValidationException("The delta must be +1."), request)

        assertThat(response.headers.contentType)
            .isEqualTo(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
    }

    @Test
    fun `the path of an error response is unknown for a request that is not a servlet request`() {
        val response = handler.handleGenericException(IllegalStateException("The check failed."), mock<WebRequest>())

        assertThat(response.body?.path).isEqualTo("unknown")
    }

    @Test
    fun `a request with an unsupported method gets 405 with the reason phrase as message and as error code`() {
        val response = handler.handleException(HttpRequestMethodNotSupportedException("DELETE"), request)

        val body = response?.body as ErrorResponse
        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        assertThat(body.message).isEqualTo("Method Not Allowed")
        assertThat(body.errorCode).isEqualTo("MethodNotAllowed")
    }

    @Test
    fun `a request body that fails Bean Validation gets 400 with each field name before its message`() {
        val bindingResult =
            BeanPropertyBindingResult(Any(), "userDto").apply {
                addError(FieldError("userDto", "loginName", "must not be null"))
                addError(FieldError("userDto", "firstName", "must not be blank"))
            }
        // the exception needs a method parameter, which the exception handler does not read
        val parameter = MethodParameter(String::class.java.getMethod("concat", String::class.java), 0)

        val response = handler.handleException(MethodArgumentNotValidException(parameter, bindingResult), request)

        val body = response?.body as ErrorResponse
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(body.errorCode).isEqualTo("BadRequest")
        assertThat(body.message).isEqualTo("loginName must not be null; firstName must not be blank")
    }

    private companion object {
        /** Returns each domain exception with the status of its error response. */
        @JvmStatic
        fun domainExceptionsAndStatuses(): Stream<Arguments> =
            Stream.of(
                Arguments.of(NotFoundException(User::class.java, UUID(0L, 1L)), HttpStatus.NOT_FOUND),
                Arguments.of(DuplicationException(User::class.java, "login name", "jane_doe"), HttpStatus.CONFLICT),
                Arguments.of(ConflictException("There is no coffee to undo."), HttpStatus.CONFLICT),
                Arguments.of(ForbiddenException("Only an admin may create a user."), HttpStatus.FORBIDDEN),
                Arguments.of(ValidationException("The delta must be +1."), HttpStatus.BAD_REQUEST)
            )
    }
}
