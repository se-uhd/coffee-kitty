package de.seuhd.campuscoffee.tests

import de.seuhd.campuscoffee.tests.SystemTestUtils.ACTING_USER_HEADER
import de.seuhd.campuscoffee.tests.SystemTestUtils.ADMIN_LOGIN_NAME
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import java.util.UUID

/**
 * The requests for the coffee count of a user, so that a step definition or a system test does not have to build an
 * HTTP request. Each function takes the id of the user, so that a test can also send a request for an unknown user. It
 * returns the [Response] with the status code, the headers, and the body or the error response. [userId] looks up the
 * id of a fixture user for a step definition, which names users by login name.
 *
 * Every request body is a map with the JSON field names, such as `mapOf("delta" to 1)`, and never a DTO of the api
 * module. A renamed field in a DTO therefore fails the tests.
 *
 * @param client the client of the server under test (see [SystemTestUtils.clientFor])
 */
class ConsumptionRequests(
    private val client: RestTestClient
) {
    /** Returns the id of the user with [loginName]. */
    fun userId(loginName: String): UUID =
        UserRequests(client)
            .retrieveAll()
            .body
            .single { it.loginName == loginName }
            .id

    /**
     * Reads the consumption of the user with [userId], which holds the coffee count.
     *
     * @param userId the id of the user
     */
    fun retrieveConsumption(userId: UUID): Response<ConsumptionJson> =
        client
            .get()
            .uri("/api/users/{userId}/consumption", userId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Adds a coffee for the user with [userId] with the body `{"delta": 1}`.
     *
     * @param userId the id of the user
     */
    fun addCoffee(userId: UUID): Response<ConsumptionJson> = applyDelta(userId, mapOf("delta" to 1))

    /**
     * Sends [body], which holds the delta, to change the coffee count of the user with [userId].
     *
     * @param userId the id of the user
     * @param body   the request body, such as `mapOf("delta" to 2)` or `mapOf("delta" to "one coffee")`
     */
    fun applyDelta(
        userId: UUID,
        body: Map<String, Any?>
    ): Response<ConsumptionJson> =
        client
            .post()
            .uri("/api/users/{userId}/consumption", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()
            .toResponse()

    /**
     * Undoes the most recent coffee of the user with [userId] (FR-3).
     *
     * @param userId the id of the user
     */
    fun undo(userId: UUID): Response<ConsumptionJson> =
        client
            .post()
            .uri("/api/users/{userId}/consumption/undo", userId)
            .exchange()
            .toResponse()

    /**
     * Reads the undo status of the user with [userId] (FR-9).
     *
     * @param userId the id of the user
     */
    fun retrieveUndoStatus(userId: UUID): Response<UndoStatusJson> =
        client
            .get()
            .uri("/api/users/{userId}/consumption/undo", userId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Corrects the coffee count of the user with [userId] to [total] with [note] (FR-8).
     *
     * @param userId     the id of the user
     * @param total      the corrected coffee count
     * @param note       the note with the reason for the correction, or null to send the field `note` as null
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun correctCount(
        userId: UUID,
        total: Int,
        note: String?,
        actingUser: String? = ADMIN_LOGIN_NAME
    ): Response<ConsumptionJson> = correctCount(userId, mapOf("total" to total, "note" to note), actingUser)

    /**
     * Sends [body], which holds the corrected coffee count and the note, to correct the coffee count of the user with
     * [userId] (FR-8). By default, the admin [ADMIN_LOGIN_NAME] is the acting user.
     *
     * @param userId     the id of the user
     * @param body       the request body, such as `mapOf("note" to "Double tap")` for a body without a total
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun correctCount(
        userId: UUID,
        body: Map<String, Any?>,
        actingUser: String? = ADMIN_LOGIN_NAME
    ): Response<ConsumptionJson> =
        client
            .put()
            .uri("/api/users/{userId}/consumption", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(ACTING_USER_HEADER, it) } }
            .body(body)
            .exchange()
            .toResponse()

    /**
     * Lists the corrections of the coffee count of the user with [userId] (FR-8), newest first. By default, the admin
     * [ADMIN_LOGIN_NAME] is the acting user.
     *
     * @param userId     the id of the user
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun retrieveCorrections(
        userId: UUID,
        actingUser: String? = ADMIN_LOGIN_NAME
    ): Response<List<CorrectionJson>> = retrieveCorrections(userId.toString(), actingUser)

    /**
     * Lists the corrections of the coffee count of the user with [userId] (FR-8), newest first. A test can pass a text
     * that is not a UUID, such as `not-a-uuid`. By default, the admin [ADMIN_LOGIN_NAME] is the acting user.
     *
     * @param userId     the id of the user
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun retrieveCorrections(
        userId: String,
        actingUser: String? = ADMIN_LOGIN_NAME
    ): Response<List<CorrectionJson>> =
        client
            .get()
            .uri("/api/users/{userId}/consumption/corrections", userId)
            .accept(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(ACTING_USER_HEADER, it) } }
            .exchange()
            .toResponse()
}
