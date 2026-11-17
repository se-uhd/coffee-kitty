package de.seuhd.campuscoffee.tests

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import org.springframework.http.MediaType
import java.util.UUID

/**
 * The requests of the system tests for users, so that a test does not have to build an HTTP request. Each function
 * returns the [Response] with the status code, the headers, and the body or the error response.
 */
object UserRequests {
    /**
     * Lists all users.
     *
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun retrieveAll(actingUser: String? = null): Response<List<UserDto>> =
        client()
            .get()
            .uri("/api/users")
            .accept(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(CurrentUserProvider.ACTING_USER_HEADER, it) } }
            .exchange()
            .toResponse()

    /**
     * Reads the user with [id].
     *
     * @param id the id of the user
     */
    fun retrieveById(id: UUID): Response<UserDto> = retrieveById(id.toString())

    /**
     * Reads the user with [id]. A test can pass a text that is not a UUID, such as `not-a-uuid`, to check the response
     * to an invalid id.
     *
     * @param id the id of the user
     */
    fun retrieveById(id: String): Response<UserDto> =
        client()
            .get()
            .uri("/api/users/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Creates a user from [body]. By default, the admin [ConsumptionRequests.ADMIN_LOGIN_NAME] is the acting user.
     *
     * @param body        the fields of the new user, such as `loginName`. A map lets a test leave out a field or add
     *   one, such as an `id`, which the server rejects.
     * @param actingUser  the login name of the acting user, or null to send the request without an acting user
     * @param queryString a query string that the request appends to the path, such as `source=test`, or null for none
     */
    fun create(
        body: Map<String, Any>,
        actingUser: String? = ConsumptionRequests.ADMIN_LOGIN_NAME,
        queryString: String? = null
    ): Response<UserDto> =
        client()
            .post()
            .uri(if (queryString == null) "/api/users" else "/api/users?$queryString")
            .contentType(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(CurrentUserProvider.ACTING_USER_HEADER, it) } }
            .body(body)
            .exchange()
            .toResponse()
}
