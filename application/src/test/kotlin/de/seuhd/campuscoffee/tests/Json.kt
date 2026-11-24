package de.seuhd.campuscoffee.tests

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.LocalDateTime
import java.util.UUID

/**
 * The JSON bodies of the REST API as the tests expect them. The system and acceptance tests read every response into
 * one of these classes and never into a DTO class of the api module. These classes therefore state the field names a
 * second time, independently of the production code. A response with a renamed, missing, or additional field fails to
 * parse, and the test fails (see [STRICT_JSON_MAPPER]).
 *
 * A timestamp is a `String`, so that a test also checks its format. [parseUtc] parses it.
 */
data class UserJson(
    val id: UUID,
    val createdAt: String,
    val updatedAt: String,
    val loginName: String,
    val emailAddress: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

/** The response body of the endpoints that read or change a coffee count. */
data class ConsumptionJson(
    val loginName: String,
    val count: Int
)

/** The response body of `GET /api/users/{userId}/consumption/undo`. [undoableUntil] is a timestamp or null. */
data class UndoStatusJson(
    val undoable: Boolean,
    val undoableUntil: String?
)

/** One element of the response body of `GET /api/users/{userId}/consumption/corrections`. */
data class CorrectionJson(
    val countBefore: Int,
    val countAfter: Int,
    val note: String,
    val adminLoginName: String,
    val createdAt: String
)

/** The body of every error response. */
data class ErrorJson(
    val errorCode: String,
    val message: String,
    val statusCode: Int,
    val statusMessage: String,
    val timestamp: String,
    val path: String
)

/**
 * The JSON mapper of the tests. It fails on a field that the target class does not have, and the Kotlin module makes
 * it fail on a missing field of a non-null type. The mapper of the app is more lenient, so a test must not read a
 * response with it.
 */
val STRICT_JSON_MAPPER: JsonMapper =
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

/**
 * Parses a timestamp of a response, such as `2026-11-16T10:00:00.123456`. The API serves every timestamp in UTC
 * without an offset. The function fails for a timestamp with an offset or a zone, such as a trailing `Z`.
 */
fun parseUtc(timestamp: String): LocalDateTime = LocalDateTime.parse(timestamp)
