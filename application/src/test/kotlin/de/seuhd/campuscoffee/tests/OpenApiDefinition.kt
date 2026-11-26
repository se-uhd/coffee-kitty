package de.seuhd.campuscoffee.tests

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult

/**
 * The statuses that the OpenAPI definition of the server documents for each endpoint. [read] fetches the definition
 * from `/v3/api-docs`, which the app serves only with `springdoc.api-docs.enabled=true`.
 *
 * @param statuses the documented statuses of each endpoint, such as 200 and 404 for `GET /api/users/{id}`
 */
class OpenApiDefinition(
    val statuses: Map<Endpoint, Set<Int>>
) {
    /** An endpoint of the definition: an HTTP method, such as `GET`, and a path template, such as `/api/users/{id}`. */
    data class Endpoint(
        val method: String,
        val path: String
    ) {
        private val pattern = Regex(path.split(PATH_VARIABLE).joinToString("[^/]+") { Regex.escape(it) })

        fun matches(
            method: String,
            path: String
        ): Boolean = method == this.method && pattern.matches(path)

        override fun toString() = "$method $path"
    }

    /**
     * Returns the endpoint that serves a request with [method] and [path], or null if the definition has none.
     *
     * @param method the HTTP method of the request, such as `GET`
     * @param path   the path of the request, such as `/api/users/not-a-uuid`
     */
    fun endpointOf(
        method: String,
        path: String
    ): Endpoint? =
        statuses.keys.filter { it.matches(method, path) }.minByOrNull { PATH_VARIABLE.findAll(it.path).count() }

    companion object {
        private val PATH_VARIABLE = Regex("\\{[^/]+}")

        /**
         * Fetches the OpenAPI definition of the server of [client].
         *
         * @param client the client of the server under test (see [SystemTestUtils.clientFor])
         */
        fun read(client: RestTestClient): OpenApiDefinition {
            val bytes =
                client
                    .get()
                    .uri("/v3/api-docs")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .returnResult<ByteArray>()
                    .responseBody
            val paths = STRICT_JSON_MAPPER.readTree(checkNotNull(bytes)).path("paths").properties()
            return OpenApiDefinition(
                paths
                    .flatMap { (path, operations) ->
                        operations.properties().map { (method, operation) ->
                            Endpoint(method.uppercase(), path) to
                                operation
                                    .path("responses")
                                    .propertyNames()
                                    .mapNotNull { it.toIntOrNull() }
                                    .toSet()
                        }
                    }.toMap()
            )
        }
    }
}
