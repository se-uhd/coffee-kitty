package de.seuhd.campuscoffee.tests

import org.springframework.http.client.ClientHttpRequestInterceptor
import java.util.concurrent.ConcurrentHashMap

/**
 * The status of every response that a test received through a client of [SystemTestUtils.clientFor], with the method
 * and the path of the request. `OpenApiDefinitionSystemTests` compares them with the OpenAPI definition.
 */
object ObservedStatuses {
    /** One response: the HTTP method and the path of the request, such as `/api/users/not-a-uuid`, and the status. */
    data class Observation(
        val method: String,
        val path: String,
        val status: Int
    )

    private val observations = ConcurrentHashMap.newKeySet<Observation>()

    /** Records the status of each response, also of a request that a test sends from another thread. */
    val interceptor =
        ClientHttpRequestInterceptor { request, body, execution ->
            execution.execute(request, body).also { response ->
                observations += Observation(request.method.name(), request.uri.rawPath, response.statusCode.value())
            }
        }

    /** Returns the responses that the tests received so far in this JVM. */
    fun all(): Set<Observation> = observations.toSet()
}
