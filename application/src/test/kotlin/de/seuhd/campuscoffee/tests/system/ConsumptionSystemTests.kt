package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.SystemTestUtils.requireStatus
import de.seuhd.campuscoffee.tests.SystemTestUtils.statusCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * System tests for a user's coffee count under `/api/users/{userId}/consumption`: reading the count and adding a
 * coffee with a delta of `+1`. The user is the fixture user `maxmustermann`.
 */
class ConsumptionSystemTests : AbstractSystemTest() {
    private val loginName = "maxmustermann"

    private fun fixtureUserId(): UUID = fixtureUser(loginName).persistedId

    private fun getConsumption(userId: UUID = fixtureUserId()) =
        client()
            .get()
            .uri("/api/users/{userId}/consumption", userId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()

    private fun postConsumption(
        body: Any,
        userId: UUID = fixtureUserId()
    ) = client()
        .post()
        .uri("/api/users/{userId}/consumption", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()

    private fun consumption(): ConsumptionDto = getConsumption().returnResult<ConsumptionDto>().responseBody!!

    @Test
    fun `getting the consumption of a fixture user returns 200 OK with a count of zero`() {
        val result = getConsumption().returnResult<ConsumptionDto>()

        assertThat(result.status.value()).isEqualTo(200)
        assertThat(result.responseBody!!.loginName).isEqualTo(loginName)
        assertThat(result.responseBody!!.count).isEqualTo(0)
    }

    @Test
    fun `adding a coffee returns 200 OK with a count of 1`() {
        val result = postConsumption(ConsumptionDeltaDto(1)).returnResult<ConsumptionDto>()

        assertThat(result.status.value()).isEqualTo(200)
        assertThat(result.responseBody!!.count).isEqualTo(1)
    }

    @Test
    fun `adding two coffees raises the count to 2`() {
        postConsumption(ConsumptionDeltaDto(1)).requireStatus(200)
        postConsumption(ConsumptionDeltaDto(1)).requireStatus(200)

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
                        postConsumption(ConsumptionDeltaDto(1), userId).statusCode()
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
    fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`() {
        val result = postConsumption(ConsumptionDeltaDto(2)).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
        assertThat(result.responseBody!!.message).isEqualTo("The delta must be +1.")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a fractional delta returns 400 Bad Request and keeps the count at zero`() {
        val result = postConsumption(mapOf("delta" to 1.9)).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a body without a delta returns 400 Bad Request`() {
        val result = postConsumption(emptyMap<String, Any>()).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `getting the consumption of an unknown user returns 404 Not Found that names the user`() {
        val userId = UUID.randomUUID()

        val result = getConsumption(userId).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFoundException")
        assertThat(result.responseBody!!.message).isEqualTo("User with ID $userId does not exist.")
    }

    @Test
    fun `adding a coffee for an unknown user returns 404 Not Found that names the user`() {
        val userId = UUID.randomUUID()

        val result = postConsumption(ConsumptionDeltaDto(1), userId).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFoundException")
        assertThat(result.responseBody!!.message).isEqualTo("User with ID $userId does not exist.")
    }

    private companion object {
        /**
         * The number of requests that the concurrency test sends at the same time, one per thread, each adding a
         * coffee. Without the row lock that `getByUserIdForUpdate` takes on the consumption, 40 such requests lose some
         * coffees, and 10 often lose none.
         */
        const val CONCURRENT_REQUESTS = 40

        /** How long the concurrency test waits for each request. */
        const val TIMEOUT_SECONDS = 30L
    }
}
