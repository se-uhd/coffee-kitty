package de.seuhd.campuscoffee.api.exceptions

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Global exception handler for all controllers, producing a standardized [ErrorResponse] body.
 *
 * Extends [ResponseEntityExceptionHandler], so the standard Spring MVC exceptions (e.g., a wrong HTTP method, an
 * unreadable body, or an unknown path) map to their own status codes instead of the generic 500 fallback, and
 * [handleExceptionInternal] renders them as [ErrorResponse]. The domain exceptions are mapped explicitly in
 * [handleMappedException], each with its class name as the error code.
 *
 * An `IllegalArgumentException` or `IllegalStateException` from a `require` or `check` guard signals a broken
 * server-side invariant, so the handler answers 500 and logs the full exception at ERROR. A log line never repeats
 * the looked-up or duplicate value of a domain exception, such as a login name, and every other text that can
 * repeat client input passes through [loggableText].
 */
@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    /**
     * Unified handler for the mapped domain exceptions. It answers with the HTTP status configured for the exception
     * type and the exception's class name as the error code, and it falls back to the generic handler for anything
     * unmapped.
     *
     * @param exception the thrown domain exception
     * @param request   the current web request
     */
    @ExceptionHandler(NotFoundException::class, DuplicationException::class, ValidationException::class)
    fun handleMappedException(
        exception: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val config = configFor(exception) ?: return handleGenericException(exception, request)
        log.info { "${config.label}: ${loggableText(logMessageOf(exception))}" }
        return respond(
            config.httpStatus,
            errorBody(config.httpStatus, request, exception.message, errorCode = exception.javaClass.simpleName)
        )
    }

    /**
     * Fallback handler for unexpected exceptions, returning HTTP 500. The invariant guards
     * (`IllegalArgumentException` and `IllegalStateException` from Kotlin's `require` and `check`) land here.
     * The response message is the fixed "An unexpected error occurred.", so no internal detail leaks, and the
     * full exception is logged at ERROR with the request's method and path.
     *
     * @param exception the unexpected exception
     * @param request   the current web request
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        exception: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(exception) { "Unexpected error on ${describe(request)}" }
        return respond(
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorBody(HttpStatus.INTERNAL_SERVER_ERROR, request, "An unexpected error occurred.")
        )
    }

    /**
     * Renders a failed validation of a `@Valid` request body as an [ErrorResponse] with the neutral `BadRequest`
     * code. The message joins the errors, each field error with its field name, and is logged at INFO.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val message =
            ex.bindingResult.allErrors.joinToString("; ") { error ->
                when (error) {
                    is FieldError -> "${error.field} ${error.defaultMessage}"
                    else -> error.defaultMessage ?: "The request is invalid."
                }
            }
        log.info { "Request validation failed: ${loggableText(message)}" }
        return respond(status, errorBody(status, request, message), headers)
    }

    /**
     * Renders every exception that [ResponseEntityExceptionHandler] handles (e.g., a malformed body, an unsupported
     * method or media type, or an unknown path) as a standard [ErrorResponse] with a neutral message and code
     * derived from the status. The framework exception's own message and class name are not echoed, because the
     * message of a body parse failure carries parser detail that can quote a value from the request body. A 4xx is
     * logged at INFO with the exception type, the status, and the request's method and path, and its message only
     * at DEBUG. A 5xx is logged at ERROR with the stack trace.
     */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            log.error(ex) { "${ex.javaClass.simpleName} -> $statusCode on ${describe(request)}" }
        } else {
            log.info { "${ex.javaClass.simpleName} -> $statusCode on ${describe(request)}" }
            log.debug { "${ex.javaClass.simpleName} detail: ${loggableText(ex.message)}" }
        }
        val reason = HttpStatus.valueOf(statusCode.value()).reasonPhrase
        return respond(statusCode, errorBody(statusCode, request, reason), headers)
    }

    /**
     * The error response with a preset `application/json;charset=UTF-8` content type, so the status and the body are
     * written whatever the request accepts, with the charset that ApiWebConfig pins on negotiated JSON.
     */
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

    /** Assembles the standardized [ErrorResponse] body for the status, request, and message, stamped in UTC. */
    private fun errorBody(
        status: HttpStatusCode,
        request: WebRequest,
        message: String?,
        errorCode: String = errorCodeFor(status)
    ): ErrorResponse =
        ErrorResponse(
            errorCode = errorCode,
            message = message,
            statusCode = status.value(),
            statusMessage = HttpStatus.valueOf(status.value()).reasonPhrase,
            timestamp = LocalDateTime.now(ZoneOffset.UTC),
            path = extractPath(request)
        )

    /** The request's method and path for a log line, never the query string. */
    private fun describe(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.let { "${it.method} ${loggableText(it.requestURI)}" }
            ?: "an unknown request"

    /** Extracts the request URI from the web request, or "unknown" when it is not a servlet request. */
    private fun extractPath(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.requestURI ?: "unknown"

    /** The exception's message for a log line, without a looked-up or duplicate value such as a login name. */
    private fun logMessageOf(exception: Exception): String? =
        when (exception) {
            is DuplicationException -> exception.logMessage
            is NotFoundException -> exception.logMessage
            else -> exception.message
        }

    /** The status and log label for a mapped domain exception, or null for anything else. */
    private fun configFor(exception: Exception): ExceptionConfig? =
        when (exception) {
            is NotFoundException -> ExceptionConfig(HttpStatus.NOT_FOUND, "Resource not found")
            is DuplicationException -> ExceptionConfig(HttpStatus.CONFLICT, "Duplicate resource")
            is ValidationException -> ExceptionConfig(HttpStatus.BAD_REQUEST, "Domain validation failed")
            else -> null
        }

    /**
     * Returns [text] as a single log line: Every control character (e.g., CR and LF) and every Unicode line or
     * paragraph separator becomes a space, and the result is cut to [MAX_LOGGED_LENGTH] characters, so a crafted
     * value cannot add a forged log entry or flood the log. Null is logged as an empty string.
     */
    private fun loggableText(text: String?): String =
        text.orEmpty().replace(LINE_BREAKING_CHARACTERS, " ").take(MAX_LOGGED_LENGTH)

    /**
     * Maps a domain exception type to its HTTP status and its log label.
     *
     * @property httpStatus the status to answer with
     * @property label the log line's prefix
     */
    private data class ExceptionConfig(
        val httpStatus: HttpStatus,
        val label: String
    )

    private companion object {
        private val log = KotlinLogging.logger {}

        /** The content type of every JSON error body, with the charset that ApiWebConfig pins on JSON. */
        private val JSON_UTF8 = MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)

        /**
         * The Unicode control characters (C0, DEL, and C1, which include CR, LF, and NEL) and the Unicode line and
         * paragraph separators.
         */
        private val LINE_BREAKING_CHARACTERS = Regex("[\\p{Cc}\\u2028\\u2029]")

        /** The longest text that one log statement keeps from a value that the client can influence. */
        private const val MAX_LOGGED_LENGTH = 500
    }
}
