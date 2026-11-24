package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.api.exceptions.errorCodeFor
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The message of a 400 for a request that the firewall of Spring Security rejects. It does not quote the request,
 * because a client can send any text in the rejected part. The coffee app of SE@UHD uses the same message.
 */
internal const val UNREADABLE_REQUEST_MESSAGE = "The request could not be read."

/**
 * Writes an [ErrorResponse] with [status] and [message] when a class of this package responds to a request in place of
 * `GlobalExceptionHandler`. The body has the same fields as the bodies that `GlobalExceptionHandler` writes.
 *
 * @param jsonMapper the JSON mapper of Spring MVC
 * @param status     the status of the response
 * @param message    the message for the client
 * @param path       the path of the request
 */
internal fun HttpServletResponse.writeErrorResponse(
    jsonMapper: JsonMapper,
    status: HttpStatus,
    message: String,
    path: String
) {
    val body =
        ErrorResponse(
            errorCode = errorCodeFor(status),
            message = message,
            statusCode = status.value(),
            statusMessage = status.reasonPhrase,
            timestamp = LocalDateTime.now(ZoneOffset.UTC),
            path = path
        )
    this.status = status.value()
    contentType = "${MediaType.APPLICATION_JSON_VALUE};charset=${StandardCharsets.UTF_8.name()}"
    writer.write(jsonMapper.writeValueAsString(body))
}
