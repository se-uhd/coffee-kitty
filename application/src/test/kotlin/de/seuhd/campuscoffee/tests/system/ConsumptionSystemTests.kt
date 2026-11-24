package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionJson
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * System tests for reading the coffee count and the undo status of a user, adding a coffee, and undoing a coffee. The
 * tests send their requests through [ConsumptionRequests]. The tests for an unknown user use a random id, and all other
 * tests use the user fixture `maxmustermann`.
 */
class ConsumptionSystemTests : AbstractSystemTest() {
    private val loginName = "maxmustermann"

    private fun userFixtureId(): UUID = userFixture(loginName).persistedId

    private fun consumption(): ConsumptionJson = consumptionRequests.retrieveConsumption(userFixtureId()).body

    @Test
    fun `getting the consumption of a user fixture returns 200 OK with a count of zero`() {
        val response = consumptionRequests.retrieveConsumption(userFixtureId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.loginName).isEqualTo(loginName)
        assertThat(response.body.count).isEqualTo(0)
    }

    @Test
    fun `adding a coffee returns 200 OK with a count of 1`() {
        val response = consumptionRequests.addCoffee(userFixtureId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.count).isEqualTo(1)
    }

    @Test
    fun `adding a coffee does not change the coffee count of another user`() {
        consumptionRequests.addCoffee(userFixtureId()).requireStatus(200)

        val other = consumptionRequests.retrieveConsumption(userFixture("lisa_lee").persistedId)

        assertThat(other.body.count).isEqualTo(0)
    }

    @Test
    fun `undoing a coffee at a count of zero returns 409 Conflict`() {
        val response = consumptionRequests.undo(userFixtureId())

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("ConflictException")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `adding coffees concurrently counts every coffee`() {
        val userId = userFixtureId()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        try {
            val statuses =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        consumptionRequests.addCoffee(userId).status
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
    fun `undoing a coffee returns 200 OK with the login name and the lowered count`() {
        repeat(3) { consumptionRequests.addCoffee(userFixtureId()).requireStatus(200) }

        val response = consumptionRequests.undo(userFixtureId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo(ConsumptionJson(loginName = loginName, count = 2))
    }

    @Test
    fun `undoing and adding coffees concurrently counts every undo and every coffee`() {
        val userId = userFixtureId()
        repeat(CONCURRENT_REQUESTS) {
            assertThat(consumptionRequests.addCoffee(userId).status).isEqualTo(200)
        }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2 * CONCURRENT_REQUESTS)
        try {
            val addRequests =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        consumptionRequests.addCoffee(userId).status
                    }
                }
            val undoRequests =
                (1..CONCURRENT_REQUESTS).map {
                    executor.submit<Int> {
                        start.await()
                        consumptionRequests.undo(userId).status
                    }
                }
            start.countDown()

            assertThat((addRequests + undoRequests).map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }).containsOnly(200)
        } finally {
            executor.shutdownNow()
        }
        assertThat(consumption().count).isEqualTo(CONCURRENT_REQUESTS)
        // Each remaining coffee is within its grace period. The user can undo each of these coffees, and the next undo
        // returns 409.
        repeat(CONCURRENT_REQUESTS) { assertThat(consumptionRequests.undo(userId).status).isEqualTo(200) }
        assertThat(consumptionRequests.undo(userId).status).isEqualTo(409)
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a delta of 2 returns 400 Bad Request`() {
        val response = consumptionRequests.applyDelta(userFixtureId(), mapOf("delta" to 2))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a fractional delta returns 400 Bad Request`() {
        val response = consumptionRequests.applyDelta(userFixtureId(), mapOf("delta" to 1.9))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a delta as a JSON string returns 400 Bad Request`() {
        val response = consumptionRequests.applyDelta(userFixtureId(), mapOf("delta" to "1"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a body without a delta returns 400 Bad Request`() {
        val response = consumptionRequests.applyDelta(userFixtureId(), emptyMap())

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `getting the consumption of an unknown user returns 404 Not Found`() {
        val userId = UUID.randomUUID()

        val response = consumptionRequests.retrieveConsumption(userId)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `adding a coffee for an unknown user returns 404 Not Found`() {
        val userId = UUID.randomUUID()

        val response = consumptionRequests.addCoffee(userId)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `undoing a coffee of an unknown user returns 404 Not Found`() {
        val userId = UUID.randomUUID()

        val response = consumptionRequests.undo(userId)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    private companion object {
        /**
         * The number of requests that a concurrency test sends at the same time from separate threads to add a coffee.
         * The test with undos also sends the same number of undos. The row lock of `getByUserIdForUpdate` prevents lost
         * coffees. Without the lock, 40 requests lose some coffees, but 10 requests often lose none.
         */
        const val CONCURRENT_REQUESTS = 40

        /** The number of seconds that a concurrency test waits for each request. */
        const val TIMEOUT_SECONDS = 30L
    }
}
