package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * System tests for reading the coffee count of a user, adding a coffee, and undoing a coffee. The tests send their
 * requests through [ConsumptionRequests]. The tests for an unknown user use a random id, and all other tests use the
 * fixture user `maxmustermann`.
 */
class ConsumptionSystemTests : AbstractSystemTest() {
    private val loginName = "maxmustermann"

    private fun fixtureUserId(): UUID = fixtureUser(loginName).persistedId

    private fun consumption(): ConsumptionDto = ConsumptionRequests.retrieveConsumption(fixtureUserId()).body

    @Test
    fun `getting the consumption of a fixture user returns 200 OK with a count of zero`() {
        val response = ConsumptionRequests.retrieveConsumption(fixtureUserId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.loginName).isEqualTo(loginName)
        assertThat(response.body.count).isEqualTo(0)
    }

    @Test
    fun `adding a coffee returns 200 OK with a count of 1`() {
        val response = ConsumptionRequests.applyDelta(fixtureUserId(), ConsumptionDeltaDto(1))

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.count).isEqualTo(1)
    }

    @Test
    fun `adding two coffees raises the count to 2`() {
        ConsumptionRequests.applyDelta(fixtureUserId(), ConsumptionDeltaDto(1)).requireStatus(200)
        ConsumptionRequests.applyDelta(fixtureUserId(), ConsumptionDeltaDto(1)).requireStatus(200)

        assertThat(consumption().count).isEqualTo(2)
    }

    @Test
    fun `adding coffees concurrently counts every coffee`() {
        val userId = fixtureUserId()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        try {
            val statuses =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        ConsumptionRequests.applyDelta(userId, ConsumptionDeltaDto(1)).status
                    }
                }
            start.countDown()

            assertThat(statuses.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }).containsOnly(200)
        } finally {
            executor.shutdownNow()
        }
        assertThat(consumption().count).isEqualTo(CONCURRENT_REQUESTS)
    }

    @Test
    fun `undoing and adding coffees concurrently keeps the count and the times of the coffees consistent`() {
        val userId = fixtureUserId()
        repeat(CONCURRENT_REQUESTS) {
            assertThat(ConsumptionRequests.applyDelta(userId, ConsumptionDeltaDto(1)).status).isEqualTo(200)
        }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2 * CONCURRENT_REQUESTS)
        try {
            val adds =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        ConsumptionRequests.applyDelta(userId, ConsumptionDeltaDto(1)).status
                    }
                }
            val undos =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        ConsumptionRequests.undo(userId).status
                    }
                }
            start.countDown()

            assertThat((adds + undos).map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }).containsOnly(200)
        } finally {
            executor.shutdownNow()
        }
        assertThat(consumption().count).isEqualTo(CONCURRENT_REQUESTS)
        // each remaining coffee is within its grace period: every one can be undone, and the next undo returns 409
        repeat(CONCURRENT_REQUESTS) { assertThat(ConsumptionRequests.undo(userId).status).isEqualTo(200) }
        assertThat(ConsumptionRequests.undo(userId).status).isEqualTo(409)
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`() {
        val response = ConsumptionRequests.applyDelta(fixtureUserId(), ConsumptionDeltaDto(2))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(response.error.message).isEqualTo("The delta must be +1.")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a fractional delta returns 400 Bad Request and keeps the count at zero`() {
        val response = ConsumptionRequests.applyDelta(fixtureUserId(), mapOf("delta" to 1.9))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a body without a delta returns 400 Bad Request`() {
        val response = ConsumptionRequests.applyDelta(fixtureUserId(), emptyMap<String, Any>())

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `getting the consumption of an unknown user returns 404 Not Found with the id of the user`() {
        val userId = UUID.randomUUID()

        val response = ConsumptionRequests.retrieveConsumption(userId)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
        assertThat(response.error.message).isEqualTo("User with ID $userId does not exist.")
    }

    @Test
    fun `adding a coffee for an unknown user returns 404 Not Found with the id of the user`() {
        val userId = UUID.randomUUID()

        val response = ConsumptionRequests.applyDelta(userId, ConsumptionDeltaDto(1))

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
        assertThat(response.error.message).isEqualTo("User with ID $userId does not exist.")
    }

    @Test
    fun `undoing a coffee of an unknown user returns 404 Not Found with the id of the user`() {
        val userId = UUID.randomUUID()

        val response = ConsumptionRequests.undo(userId)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
        assertThat(response.error.message).isEqualTo("User with ID $userId does not exist.")
    }

    private companion object {
        /**
         * The number of requests that the concurrency test sends at the same time from separate threads. Each request
         * adds a coffee. Without the row lock that `getByUserIdForUpdate` sets on the consumption, 40 requests lose
         * some coffees, but 10 requests often lose none.
         */
        const val CONCURRENT_REQUESTS = 40

        /** How long the concurrency test waits for each request. */
        const val TIMEOUT_SECONDS = 30L
    }
}
