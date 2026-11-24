package de.seuhd.campuscoffee.tests

import de.seuhd.campuscoffee.tests.SystemTestUtils.ACTING_USER_HEADER
import de.seuhd.campuscoffee.tests.SystemTestUtils.ADMIN_LOGIN_NAME
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import java.net.URI
import java.util.UUID

/**
 * The requests of the system tests for users, so that a test does not have to build an HTTP request. Each function
 * returns the [Response] with the status code, the headers, and the body or the error response.
 *
 * @param client the client of the server under test (see [SystemTestUtils.clientFor])
 */
class UserRequests(
    private val client: RestTestClient
) {
    /**
     * Lists all users.
     *
     * @param actingUser the login name of the acting user, or null to send the request without an acting user
     */
    fun retrieveAll(actingUser: String? = null): Response<List<UserJson>> =
        client
            .get()
            .uri("/api/users")
            .accept(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(ACTING_USER_HEADER, it) } }
            .exchange()
            .toResponse()

    /**
     * Reads the user with [id].
     *
     * @param id the id of the user
     */
    fun retrieveById(id: UUID): Response<UserJson> = retrieveById(id.toString())

    /**
     * Reads the user with [id]. A test can pass a text that is not a UUID, such as `not-a-uuid`.
     *
     * @param id the id of the user
     */
    fun retrieveById(id: String): Response<UserJson> =
        client
            .get()
            .uri("/api/users/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Reads the user at [location], the URL in the `Location` header of a response.
     *
     * @param location the URL to read
     */
    fun retrieveAt(location: URI): Response<UserJson> =
        client
            .get()
            .uri(location)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .toResponse()

    /**
     * Creates a user from [body]. By default, the admin [ADMIN_LOGIN_NAME] is the acting user.
     *
     * @param body        the fields of the new user by their JSON names, such as `loginName`
     * @param actingUser  the login name of the acting user, or null to send the request without an acting user
     * @param queryString a query string that the request appends to the path, such as `source=test`, or null for none
     */
    fun create(
        body: Map<String, Any?>,
        actingUser: String? = ADMIN_LOGIN_NAME,
        queryString: String? = null
    ): Response<UserJson> =
        client
            .post()
            .uri(if (queryString == null) "/api/users" else "/api/users?$queryString")
            .contentType(MediaType.APPLICATION_JSON)
            .headers { headers -> actingUser?.let { headers.set(ACTING_USER_HEADER, it) } }
            .body(body)
            .exchange()
            .toResponse()
}
