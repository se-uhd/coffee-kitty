package de.seuhd.campuscoffee.api.exceptions

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.time.LocalDateTime

/**
 * The JSON body of an error response.
 *
 * @param errorCode     the class name of a domain exception or, for any other error, the reason phrase without spaces
 * @param message       the error message for the client, such as the message of a domain exception
 * @param statusCode    the HTTP status code (e.g., 400, 404, or 500)
 * @param statusMessage the reason phrase of the status (e.g., "Bad Request" or "Not Found")
 * @param timestamp     when the error occurred, in UTC without an offset
 * @param path          the path of the request that caused the error
 */
data class ErrorResponse(
    @field:Schema(
        description =
            "The class name of the domain exception, or the reason phrase of the status without spaces, such as " +
                "BadRequest.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val errorCode: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val message: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val statusCode: Int,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val statusMessage: String,
    @field:Schema(description = "When the error occurred, in UTC.", requiredMode = Schema.RequiredMode.REQUIRED)
    val timestamp: LocalDateTime,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
