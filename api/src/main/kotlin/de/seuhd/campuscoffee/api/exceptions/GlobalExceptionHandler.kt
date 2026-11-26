package de.seuhd.campuscoffee.api.exceptions

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.DomainException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.PropertyBindingException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The exception handler for all controllers. The body of every error response is an [ErrorResponse].
 * [handleDomainException] maps each domain exception to a status and uses the class name of the exception as the error
 * code. The base class [ResponseEntityExceptionHandler] maps the standard Spring MVC exceptions to their statuses, and
 * [handleGenericException] returns a 500 for every other exception. Every log message that can contain client input,
 * such as the request path, passes through [loggableText], except the stack traces at ERROR.
 */
@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    /**
     * Returns an error response for a domain exception, with the status from [statusOf] and the class name of the
     * exception as the error code. To add a domain exception, create a subclass of [DomainException] and add its status
     * to [statusOf].
     *
     * @param exception the domain exception
     * @param request   the current web request
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        exception: DomainException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val status = statusOf(exception)
        val errorCode = exception.javaClass.simpleName
        log.info { "$errorCode -> $status on ${describe(request)}: ${loggableText(exception.logMessage)}" }
        return respond(status, errorBody(status, request, exception.message, errorCode))
    }

    /**
     * Returns a 500 with a fixed message for every exception that no other handler maps. Spring throws an
     * `IllegalArgumentException` for an asterisk as the type or subtype in the `Content-Type` header before it calls a
     * message converter, so the handler returns 415 for it, as for any other media type that the API does not read.
     *
     * @param exception the exception that no other handler maps
     * @param request   the current web request
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        exception: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        if (exception is IllegalArgumentException && request.hasWildcardContentType()) {
            val status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
            log.info { "Wildcard Content-Type -> $status on ${describe(request)}" }
            return respond(status, errorBody(status, request, status.reasonPhrase))
        }
        log.error(exception) { "Unexpected error on ${describe(request)}" }
        return respond(
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorBody(HttpStatus.INTERNAL_SERVER_ERROR, request, "An unexpected error occurred.")
        )
    }

    /**
     * Returns a 400 with the error code `BadRequest` for a request body that Jackson cannot read. When Jackson fails at
     * a field of the DTO, the message names this field (see [unreadableField]). Every other body that Jackson cannot
     * read, such as invalid JSON or a body with an unknown field, gets the response of [handleExceptionInternal].
     */
    override fun handleHttpMessageNotReadable(
        exception: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val field =
            unreadableField(exception) ?: return handleExceptionInternal(exception, null, headers, status, request)
        val message = "The field $field is missing or has a value that the server cannot read."
        log.info { "${exception.javaClass.simpleName} -> $status on ${describe(request)}: $message" }
        return respond(status, errorBody(status, request, message), headers)
    }

    /**
     * Builds the error response for an exception that [ResponseEntityExceptionHandler] maps to a status, with the
     * reason phrase of the status as the message. The response leaves out the message of the exception, because it can
     * quote client input, such as a value from a body that Jackson cannot read.
     */
    override fun handleExceptionInternal(
        exception: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            log.error(exception) { "${exception.javaClass.simpleName} -> $statusCode on ${describe(request)}" }
        } else {
            log.info { "${exception.javaClass.simpleName} -> $statusCode on ${describe(request)}" }
            log.debug { "${exception.javaClass.simpleName} detail: ${loggableText("${exception.message}")}" }
        }
        val reason = HttpStatus.valueOf(statusCode.value()).reasonPhrase
        return respond(statusCode, errorBody(statusCode, request, reason), headers)
    }

    private fun <T : Any> respond(
        status: HttpStatusCode,
        body: T,
        headers: HttpHeaders? = null
    ): ResponseEntity<T> =
        ResponseEntity
            .status(status)
            .headers(headers)
            .contentType(JSON_UTF8)
            .body(body)

    private fun errorBody(
        status: HttpStatusCode,
        request: WebRequest,
        message: String,
        errorCode: String = errorCodeFor(status)
    ): ErrorResponse =
        ErrorResponse(
            errorCode = errorCode,
            message = message,
            statusCode = status.value(),
            statusMessage = HttpStatus.valueOf(status.value()).reasonPhrase,
            timestamp = LocalDateTime.now(ZoneOffset.UTC),
            path = request.servletRequest().requestURI
        )

    // ADR-008
    private fun unreadableField(exception: HttpMessageNotReadableException): String? {
        val cause = exception.cause
        if (cause !is MismatchedInputException || cause is PropertyBindingException) {
            return null
        }
        return cause.path.lastOrNull()?.propertyName
    }

    private fun describe(request: WebRequest): String =
        request.servletRequest().let { "${it.method} ${loggableText(it.requestURI)}" }

    private fun WebRequest.servletRequest(): HttpServletRequest = (this as ServletWebRequest).request

    private fun statusOf(exception: DomainException): HttpStatus =
        when (exception) {
            is NotFoundException -> HttpStatus.NOT_FOUND
            is DuplicationException, is ConflictException -> HttpStatus.CONFLICT
            is ForbiddenException -> HttpStatus.FORBIDDEN
            is ValidationException -> HttpStatus.BAD_REQUEST
        }

    // ADR-015
    private fun loggableText(text: String): String =
        text.replace(CONTROL_AND_LINE_SEPARATOR_CHARACTERS, " ").take(MAX_LOGGED_LENGTH)

    private companion object {
        private val log = KotlinLogging.logger {}

        private val JSON_UTF8 = MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)

        private val CONTROL_AND_LINE_SEPARATOR_CHARACTERS = Regex("[\\p{Cc}\\u2028\\u2029]")

        // ADR-010
        private const val MAX_LOGGED_LENGTH = 500
    }
}

private fun WebRequest.hasWildcardContentType(): Boolean =
    '*' in getHeader(HttpHeaders.CONTENT_TYPE).orEmpty().substringBefore(';')
