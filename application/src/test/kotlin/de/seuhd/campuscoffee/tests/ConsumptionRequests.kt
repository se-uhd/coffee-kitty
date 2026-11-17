package de.seuhd.campuscoffee.tests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.SystemTestUtils.statusCode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.util.UUID

/**
 * The requests for the coffee count of a user, so that a step definition or a system test does not have to build an
 * HTTP request. The functions for the step definitions take the login names of fixture users and return the status
 * code of the response or the values that a step checks. The functions for the system tests, [retrieveConsumption],
 * [applyDelta], and [undo], take the id of a user, so that a test can also send a request for an unknown user. They
 * return the [Response] with the status code, the headers, and the body or the error response.
 *
 * [correctCoffeeCount] and [corrections] send the requests of FR-8 (`requirements/FR-8-correct-coffee-count.md`),
 * which Coffee Kitty 0.2.0 does not implement yet. Until an implementation adds them, [correctCoffeeCount] returns
 * 405 Method Not Allowed and [corrections] fails with the status 404 Not Found.
 */
object ConsumptionRequests {
    /** The login name of the fixture user who is an admin. */
    const val ADMIN_LOGIN_NAME = "jane_doe"

    /** One correction of a coffee count, as [corrections] returns it. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Correction(
        val countBefore: Int,
        val countAfter: Int,
        val note: String,
        val adminLoginName: String
    )

    /** Returns the id of the user with [loginName]. */
    fun userId(loginName: String): UUID {
        val users =
            client()
                .get()
                .uri("/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<Array<UserDto>>()
                .responseBody!!
        return users.single { it.loginName == loginName }.persistedId
    }

    /** Returns the coffee count of the user with [loginName]. */
    fun coffeeCount(loginName: String): Int =
        client()
            .get()
            .uri("/api/users/{userId}/consumption", userId(loginName))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<ConsumptionDto>()
            .responseBody!!
            .count

    /** Adds a coffee for the user with [loginName] and returns the status code, 200 on success. */
    fun addCoffee(loginName: String): Int =
        client()
            .post()
            .uri("/api/users/{userId}/consumption", userId(loginName))
            .contentType(MediaType.APPLICATION_JSON)
            .body(ConsumptionDeltaDto(1))
            .exchange()
            .statusCode()

    /** Undoes the most recent coffee of the user with [loginName] and returns the status code (FR-3). */
    fun undoCoffee(loginName: String): Int =
        client()
            .post()
            .uri("/api/users/{userId}/consumption/undo", userId(loginName))
            .exchange()
            .statusCode()

    /**
     * Sends the correction of FR-8: [actingUser] corrects the coffee count of the user with [loginName] to [count] with
     * [note], or without a note if [note] is null. Returns the status code.
     */
    fun correctCoffeeCount(
        actingUser: String,
        loginName: String,
        count: Int,
        note: String?
    ): Int =
        client()
            .put()
            .uri("/api/users/{userId}/consumption", userId(loginName))
            .header(CurrentUserProvider.ACTING_USER_HEADER, actingUser)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("total" to count, "note" to note))
            .exchange()
            .statusCode()

    /**
     * Returns the corrections of the coffee count of the user with [loginName] (FR-8), newest first. The admin
     * [ADMIN_LOGIN_NAME] reads them.
     */
    fun corrections(loginName: String): List<Correction> =
        client()
            .get()
            .uri("/api/users/{userId}/consumption/corrections", userId(loginName))
            .header(CurrentUserProvider.ACTING_USER_HEADER, ADMIN_LOGIN_NAME)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<Array<Correction>>()
            .responseBody!!
            .toList()

    /**
     * Reads the consumption of the user with [userId], which holds the coffee count.
     *
     * @param userId the id of the user
     */
    fun retrieveConsumption(userId: UUID): Response<ConsumptionDto> =
        client()
            .get()
            .uri("/api/users/{userId}/consumption", userId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Sends [body], which holds the delta, to change the coffee count of the user with [userId]. A body of
     * `ConsumptionDeltaDto(1)` adds a coffee.
     *
     * @param userId the id of the user
     * @param body   the request body, such as `ConsumptionDeltaDto(1)`. A map lets a test send a body that
     *   `ConsumptionDeltaDto` cannot hold, such as `mapOf("delta" to 1.9)`, which the server rejects.
     */
    fun applyDelta(
        userId: UUID,
        body: Any
    ): Response<ConsumptionDto> =
        client()
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
    fun undo(userId: UUID): Response<ConsumptionDto> =
        client()
            .post()
            .uri("/api/users/{userId}/consumption/undo", userId)
            .exchange()
            .toResponse()
}
