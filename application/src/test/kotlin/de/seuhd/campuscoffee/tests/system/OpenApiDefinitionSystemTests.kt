package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.tests.ObservedStatuses
import de.seuhd.campuscoffee.tests.OpenApiDefinition
import de.seuhd.campuscoffee.tests.OpenApiDefinition.Endpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

/**
 * Compares the OpenAPI definition with the statuses that the other tests received ([ObservedStatuses]). The class runs
 * after the other test classes of JUnit Jupiter (`junit-platform.properties`), so run it with the other system tests.
 * `ErrorResponsesCustomizer` documents each of [RULE_STATUSES] on every endpoint to which its rule applies, so one test
 * of the status suffices. Every other documented status needs a test of its endpoint or an entry in [NOT_YET_TESTED].
 */
@Order(Int.MAX_VALUE)
@TestPropertySource(properties = ["springdoc.api-docs.enabled=true"])
class OpenApiDefinitionSystemTests : AbstractSystemTest() {
    private val definition by lazy { OpenApiDefinition.read(client) }

    private fun observed(): Set<Pair<Endpoint, Int>> =
        ObservedStatuses
            .all()
            .mapNotNull { response ->
                definition.endpointOf(response.method, response.path)?.let { it to response.status }
            }.toSet()

    @Test
    fun `the OpenAPI definition documents every status that the tests observe`() {
        val undocumented = observed().filter { (endpoint, status) -> status !in definition.statuses.getValue(endpoint) }

        assertThat(undocumented).`as`("observed statuses that the OpenAPI definition does not document").isEmpty()
    }

    @Test
    fun `the tests observe every status that the OpenAPI definition documents`() {
        val observed = observed()
        val documented = definition.statuses.flatMap { (endpoint, statuses) -> statuses.map { endpoint to it } }

        val untested =
            documented.filterNot { (endpoint, status) ->
                (endpoint to status) in observed ||
                    (endpoint to status) in NOT_YET_TESTED ||
                    (status in RULE_STATUSES && observed.any { it.second == status })
            }

        assertThat(untested).`as`("documented statuses that no test of this run observed").isEmpty()
    }

    @Test
    fun `the tests observe no status of NOT_YET_TESTED`() {
        assertThat(NOT_YET_TESTED.filter { it in observed() }).`as`("tested statuses in NOT_YET_TESTED").isEmpty()
    }

    private companion object {
        /** The statuses that `ErrorResponsesCustomizer` derives from a rule of the api module, not from the domain. */
        val RULE_STATUSES = setOf(400, 401, 406, 415, 500)

        /** The documented statuses without a test. The tests of the undo status (FR-9) come on 2026-12-08. */
        val NOT_YET_TESTED = setOf(200, 404).map { Endpoint("GET", "/api/users/{userId}/consumption/undo") to it }
    }
}
