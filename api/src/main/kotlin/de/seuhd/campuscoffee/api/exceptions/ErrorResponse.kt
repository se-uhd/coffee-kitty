package de.seuhd.campuscoffee.api.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.time.LocalDateTime

/**
 * The JSON body of every error response that the API returns.
 *
 * @param errorCode     the class name of a domain exception (e.g., `NotFoundException`) or, for any other error, the
 *   reason phrase of the status without spaces (e.g., `BadRequest`)
 * @param message       a human-readable error message
 * @param statusCode    the HTTP status code (e.g., 400, 404, or 500)
 * @param statusMessage the reason phrase of the status (e.g., "Bad Request" or "Not Found")
 * @param timestamp     when the error occurred, in UTC without an offset, like every API timestamp
 * @param path          the path of the request that caused the error
 */
data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val statusCode: Int,
    val statusMessage: String,
    val timestamp: LocalDateTime,
    val path: String
)

/**
 * The neutral error code for [status], its reason phrase without spaces (e.g., `BadRequest`), used for every
 * error that is not a domain exception, so that no exception class name outside the domain reaches a client.
 *
 * @param status the response status
 */
internal fun errorCodeFor(status: HttpStatusCode): String =
    HttpStatus.valueOf(status.value()).reasonPhrase.replace(" ", "")
