package de.seuhd.campuscoffee.api.exceptions

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.time.LocalDateTime

/**
 * Standardized error response for all API exceptions.
 *
 * @param errorCode     the domain exception's class name (e.g., NotFoundException) for a domain error, otherwise
 *   the status reason phrase without spaces (e.g., BadRequest)
 * @param message       human-readable error message; null is allowed and omitted from the JSON
 * @param statusCode    HTTP status code (e.g., 400, 404, 500)
 * @param statusMessage HTTP status message (e.g., "Bad Request", "Not Found")
 * @param timestamp     when the error occurred, in UTC like every API timestamp
 * @param path          request path that caused the error
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // excludes null fields from JSON
data class ErrorResponse(
    val errorCode: String,
    val message: String?,
    val statusCode: Int,
    val statusMessage: String? = null,
    val timestamp: LocalDateTime? = null,
    val path: String? = null
)

/**
 * The neutral error code for [status], its reason phrase without spaces (e.g., `BadRequest`), used for every
 * error that is not a domain exception, so that no exception class name outside the domain reaches a client.
 *
 * @param status the response status
 */
internal fun errorCodeFor(status: HttpStatusCode): String =
    HttpStatus.valueOf(status.value()).reasonPhrase.replace(" ", "")
