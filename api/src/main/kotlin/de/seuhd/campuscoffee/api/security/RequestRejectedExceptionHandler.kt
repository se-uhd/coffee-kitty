package de.seuhd.campuscoffee.api.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.web.firewall.RequestRejectedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tools.jackson.databind.json.JsonMapper

/**
 * Answers a [RequestRejectedException] that a controller throws with 400 Bad Request and an `ErrorResponse`. The
 * firewall of Spring Security checks a header value, such as one with a control character, only when the code reads
 * the header, so the exception can reach a controller. A path that the firewall rejects never reaches a controller,
 * and `SecurityConfig.requestRejectedHandler` answers it with the same 400. The advice has the highest precedence,
 * because `GlobalExceptionHandler` would answer the exception with a 500. The message does not quote the request, and
 * the log names only the exception type, as in the coffee app of SE@UHD.
 *
 * @param jsonMapper the JSON mapper of Spring MVC
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestRejectedExceptionHandler(
    private val jsonMapper: JsonMapper
) {
    /**
     * Writes the 400 for [exception].
     *
     * @param exception the exception of the firewall
     * @param request   the current request
     * @param response  the response that the handler writes
     */
    @ExceptionHandler(RequestRejectedException::class)
    fun handleRequestRejected(
        exception: RequestRejectedException,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        log.info { "Rejected request on ${request.method}: ${exception::class.simpleName}" }
        response.writeErrorResponse(jsonMapper, HttpStatus.BAD_REQUEST, UNREADABLE_REQUEST_MESSAGE, request.requestURI)
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
