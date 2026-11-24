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
 * The exception handler for all controllers. The body of every error response is an [ErrorResponse].
 *
 * [handleDomainException] maps each domain exception to a status and uses the class name of the exception as the
 * error code. The base class [ResponseEntityExceptionHandler] maps each standard Spring MVC exception to its status
 * code. Examples are the exceptions for an unsupported HTTP method, a body that Jackson cannot read, and an unknown
 * path. [handleExceptionInternal] builds the error response for the standard Spring MVC exceptions.
 * [handleGenericException] returns a 500 for every other exception.
 *
 * A failed `require` or `check` throws an `IllegalArgumentException` or an `IllegalStateException`. Such an exception
 * indicates a bug in the server code, and [handleGenericException] logs the exception with its stack trace at ERROR.
 * The log line for a domain exception does not contain a looked-up or duplicate value, such as a login name. The
 * handler passes every log message that can contain client input, such as the request path, through [loggableText].
 * The stack traces that the handler logs at ERROR do not pass through [loggableText].
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
     * Returns a 500 for every exception that no other handler maps, such as an `IllegalArgumentException` from a failed
     * `require`. Every such response has the fixed message "An unexpected error occurred." The log line at ERROR
     * contains the method and path of the request and the exception with its stack trace.
     *
     * One `IllegalArgumentException` is an error of the client. Spring throws it for a request with a wildcard in the
     * `Content-Type` header, that is, an asterisk as the type or as the subtype. At that point, Spring has not called a
     * message converter yet. Such a request gets a 415 with the error code `UnsupportedMediaType`, like a request with
     * any other media type that the API does not read. The handler logs the request at INFO.
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
     * Returns a 400 with the error code `BadRequest` for a request body that fails Bean Validation (`@Valid`). The
     * message contains the field errors, separated by "; ", and each field error starts with the field name. An error
     * of a constraint on the class would have no field name, but no DTO has such a constraint. The handler logs the
     * message at INFO.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val message =
            ex.bindingResult.fieldErrors.joinToString("; ") { error -> "${error.field} ${error.defaultMessage}" }
        log.info { "Request validation failed on ${describe(request)}: ${loggableText(message)}" }
        return respond(status, errorBody(status, request, message), headers)
    }

    /**
     * Builds the error response for an exception that [ResponseEntityExceptionHandler] maps to a status. Examples are
     * the exceptions for a body that Jackson cannot read, an unsupported method or media type, and an unknown path.
     * [handleMethodArgumentNotValid] builds the error response for a request body that fails Bean Validation. The error
     * response has the reason phrase of the status as its message and the same phrase without spaces as its error code.
     * It does not contain the message or the class name of the exception. The message of such an exception can quote
     * client input, for example a value from a body that Jackson cannot read.
     *
     * The handler logs a 4xx at INFO with the class name of the exception, the status, and the method and path of the
     * request. It logs the message of the exception at DEBUG. It logs a 5xx at ERROR with the stack trace.
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
            log.debug { "${ex.javaClass.simpleName} detail: ${loggableText("${ex.message}")}" }
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

    /**
     * Builds the [ErrorResponse] for [status], [request], and [message], with the current UTC time as the timestamp.
     */
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

    /** Returns the method and path of [request] for a log line, without the query string. */
    private fun describe(request: WebRequest): String =
        request.servletRequest().let { "${it.method} ${loggableText(it.requestURI)}" }

    /** Returns the servlet request of this request. Spring MVC passes a [ServletWebRequest] to an exception handler. */
    private fun WebRequest.servletRequest(): HttpServletRequest = (this as ServletWebRequest).request

    /** Returns the status of the error response for [exception]. */
    private fun statusOf(exception: DomainException): HttpStatus =
        when (exception) {
            is NotFoundException -> HttpStatus.NOT_FOUND
            is DuplicationException, is ConflictException -> HttpStatus.CONFLICT
            is ForbiddenException -> HttpStatus.FORBIDDEN
            is ValidationException -> HttpStatus.BAD_REQUEST
        }

    /**
     * Returns [text] as a single log line. The function replaces every control character, such as CR and LF, and every
     * Unicode line or paragraph separator with a space. A client therefore cannot forge a log entry with a line break.
     * The function then cuts the result to [MAX_LOGGED_LENGTH] characters, because a client could otherwise fill the
     * log with a long value.
     */
    private fun loggableText(text: String): String =
        text.replace(CONTROL_AND_LINE_SEPARATOR_CHARACTERS, " ").take(MAX_LOGGED_LENGTH)

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

        /** The maximum length of a text that [loggableText] returns. */
        private const val MAX_LOGGED_LENGTH = 500
    }
}

/**
 * Returns whether the media type in the `Content-Type` header has an asterisk as its type or as its subtype. An
 * asterisk before a suffix, such as `+json`, also counts. A request without the header has no wildcard.
 */
private fun WebRequest.hasWildcardContentType(): Boolean =
    '*' in getHeader(HttpHeaders.CONTENT_TYPE).orEmpty().substringBefore(';')
