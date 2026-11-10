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
 * Exception handler for all controllers. The body of every error response is an [ErrorResponse].
 *
 * [handleMappedException] maps each domain exception to a status and uses the class name of the exception as the
 * error code. The base class [ResponseEntityExceptionHandler] maps each standard Spring MVC exception to its status
 * code. Examples are the exceptions for a wrong HTTP method, an unreadable body, and an unknown path.
 * [handleExceptionInternal] builds the error response for the standard Spring MVC exceptions. [handleGenericException]
 * returns a 500 for every other exception.
 *
 * A failed `require` or `check` throws an `IllegalArgumentException` or an `IllegalStateException`. Such an exception
 * indicates a bug in the server code, and [handleGenericException] logs the exception with its stack trace at ERROR.
 * The log line for a domain exception does not contain a looked-up or duplicate value, such as a login name. The
 * handler passes every log message that can contain client input, such as the request path, through [loggableText].
 * The stack traces that it logs at ERROR are not cleaned.
 */
@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    /**
     * Returns an error response for a domain exception, with the status from [configFor] and the class name of the
     * exception as the error code. The method calls [handleGenericException] for an exception that [configFor] does
     * not map.
     *
     * To add a domain exception, list its class in the `@ExceptionHandler` annotation of this method and add an entry
     * to [configFor]. If the class or the entry is missing, the client gets a 500. If the message of the new exception
     * can contain a value from the client, such as a login name, add a branch to [logMessageOf] that returns a message
     * without that value.
     *
     * @param exception the domain exception
     * @param request   the current web request
     */
    @ExceptionHandler(NotFoundException::class, DuplicationException::class, ValidationException::class)
    fun handleMappedException(
        exception: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val config = configFor(exception) ?: return handleGenericException(exception, request)
        log.info { "${config.label} on ${describe(request)}: ${loggableText(logMessageOf(exception))}" }
        return respond(
            config.httpStatus,
            errorBody(
                config.httpStatus,
                request,
                exception.message.orEmpty(),
                errorCode = exception.javaClass.simpleName
            )
        )
    }

    /**
     * Returns a 500 for every exception that no other handler maps, such as an `IllegalArgumentException` from a failed
     * `require`. Every such response has the fixed message "An unexpected error occurred." The log line at ERROR
     * contains the method and path of the request and the exception with its stack trace.
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
     * Returns a 400 with the error code `BadRequest` for a request body that fails Bean Validation (`@Valid`). The
     * message joins the validation errors with "; ", and each field error starts with the field name. The handler
     * logs the message at INFO.
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
        log.info { "Request validation failed on ${describe(request)}: ${loggableText(message)}" }
        return respond(status, errorBody(status, request, message), headers)
    }

    /**
     * Builds the error response for an exception that [ResponseEntityExceptionHandler] handles, such as the exception
     * for a malformed body, an unsupported method or media type, or an unknown path. [handleMethodArgumentNotValid]
     * handles a failed Bean Validation instead. The error response has the reason phrase of the status as its message
     * and the same phrase without spaces as its error code. The error response does not contain the message or the
     * class name of the exception, because the message for a body that cannot be parsed can quote a value from the
     * body.
     *
     * A 4xx is logged at INFO with the class name of the exception, the status, and the method and path of the
     * request. The message of the exception is logged at DEBUG. A 5xx is logged at ERROR with the stack trace.
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
     * Returns a response with [status], [body], and the content type `application/json;charset=UTF-8`. The client gets
     * the JSON body even when its `Accept` header does not list JSON, because Spring skips content negotiation when
     * the response already has a content type.
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

    /** Builds the [ErrorResponse] for [status], [request], and [message], with the current UTC time as timestamp. */
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
            path = extractPath(request)
        )

    /** Returns the method and path of [request] for a log line, without the query string. */
    private fun describe(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.let { "${it.method} ${loggableText(it.requestURI)}" }
            ?: "an unknown request"

    /** Returns the path of [request], or "unknown" for a request that is not a servlet request. */
    private fun extractPath(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.requestURI ?: "unknown"

    /** Returns the message of [exception] for the log, without a looked-up or duplicate value, such as a login name. */
    private fun logMessageOf(exception: Exception): String? =
        when (exception) {
            is DuplicationException -> exception.logMessage
            is NotFoundException -> exception.logMessage
            else -> exception.message
        }

    /** Returns the status and the log label for a domain exception, or null for any other exception. */
    private fun configFor(exception: Exception): ExceptionConfig? =
        when (exception) {
            is NotFoundException -> ExceptionConfig(HttpStatus.NOT_FOUND, "Resource not found")
            is DuplicationException -> ExceptionConfig(HttpStatus.CONFLICT, "Duplicate resource")
            is ValidationException -> ExceptionConfig(HttpStatus.BAD_REQUEST, "Domain validation failed")
            else -> null
        }

    /**
     * Returns [text] as a single log line. The function replaces every control character, such as CR and LF, and
     * every Unicode line or paragraph separator with a space. This replacement stops a client from forging a log entry
     * with a line break. The function then cuts the result to [MAX_LOGGED_LENGTH] characters to stop a long value from
     * flooding the log. Null becomes an empty string.
     */
    private fun loggableText(text: String?): String =
        text.orEmpty().replace(CONTROL_AND_LINE_SEPARATOR_CHARACTERS, " ").take(MAX_LOGGED_LENGTH)

    /**
     * The HTTP status and the log label that [configFor] assigns to a domain exception.
     *
     * @property httpStatus the status of the error response
     * @property label the prefix of the log line
     */
    private data class ExceptionConfig(
        val httpStatus: HttpStatus,
        val label: String
    )

    private companion object {
        private val log = KotlinLogging.logger {}

        /** The content type of every error response, with the charset that ApiWebConfig adds to JSON responses. */
        private val JSON_UTF8 = MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)

        /**
         * Matches a character of the Unicode category Cc, the line separator U+2028, or the paragraph separator
         * U+2029. The category Cc contains the C0 controls U+0000 to U+001F, DEL, and the C1 controls U+0080 to U+009F.
         * CR and LF are C0 controls, and NEL is a C1 control.
         */
        private val CONTROL_AND_LINE_SEPARATOR_CHARACTERS = Regex("[\\p{Cc}\\u2028\\u2029]")

        /** The maximum number of characters that [loggableText] keeps of a text that can contain client input. */
        private const val MAX_LOGGED_LENGTH = 500
    }
}
