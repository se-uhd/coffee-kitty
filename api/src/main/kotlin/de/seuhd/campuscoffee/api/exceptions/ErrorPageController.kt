package de.seuhd.campuscoffee.api.exceptions

import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Answers the error page `/error` with an [ErrorResponse] for every method and `Accept` header, in place of the error
 * controller of Spring Boot. Tomcat forwards a request to the error page when it ends in an error outside Spring MVC,
 * such as an exception of a filter, which the code that threw it has logged. A direct request has no error status and
 * gets 404. The OpenAPI definition leaves the error page out.
 */
@Hidden
@RestController
@RequestMapping("/error")
class ErrorPageController : ErrorController {
    @RequestMapping
    fun error(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val status =
            (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int)?.let { HttpStatus.resolve(it) }
                ?: HttpStatus.NOT_FOUND
        val message = if (status.is5xxServerError) "An unexpected error occurred." else status.reasonPhrase
        val body =
            ErrorResponse(
                errorCode = errorCodeFor(status),
                message = message,
                statusCode = status.value(),
                statusMessage = status.reasonPhrase,
                timestamp = LocalDateTime.now(ZoneOffset.UTC),
                path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String ?: request.requestURI
            )
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(body)
    }
}
