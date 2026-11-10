package de.seuhd.campuscoffee.api.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.time.LocalDateTime

/**
 * The JSON body of an error response.
 *
 * @param errorCode     the class name of a domain exception (e.g., `NotFoundException`) or, for any other error, the
 *   reason phrase of the status without spaces (e.g., `BadRequest`)
 * @param message       the error message for the client, such as the message of a domain exception
 * @param statusCode    the HTTP status code (e.g., 400, 404, or 500)
 * @param statusMessage the reason phrase of the status (e.g., "Bad Request" or "Not Found")
 * @param timestamp     when the error occurred, in UTC without an offset
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
 * Returns the error code for an error that is not a domain exception: the reason phrase of [status] without spaces,
 * such as `BadRequest`. Because the error code depends only on the status, a client never sees the class name of an
 * exception from outside the domain.
 *
 * @param status the status of the response
 */
internal fun errorCodeFor(status: HttpStatusCode): String =
    HttpStatus.valueOf(status.value()).reasonPhrase.replace(" ", "")
