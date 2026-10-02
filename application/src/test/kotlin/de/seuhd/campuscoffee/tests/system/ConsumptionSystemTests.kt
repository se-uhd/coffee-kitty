package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.SystemTestUtils.requireStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult
import java.util.UUID

/**
 * System tests for a user's coffee count under `/api/users/{userId}/consumption`: reading the count and adding a
 * coffee with a delta of `+1`. The user is the fixture user `maxmustermann`.
 */
class ConsumptionSystemTests : AbstractSystemTest() {
    private val loginName = "maxmustermann"

    private fun fixtureUserId(): UUID = seededUser(loginName).persistedId

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
    fun `getting the consumption of a fixture user returns 200 with a count of zero`() {
        val result = getConsumption().returnResult<ConsumptionDto>()

        assertThat(result.status.value()).isEqualTo(200)
        assertThat(result.responseBody!!.loginName).isEqualTo(loginName)
        assertThat(result.responseBody!!.count).isEqualTo(0)
    }

    @Test
    fun `adding a coffee returns 200 with a count of 1`() {
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
    fun `posting a delta of 2 returns 400 Bad Request and keeps the count at zero`() {
        val result = postConsumption(ConsumptionDeltaDto(2)).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("ValidationException")
        assertThat(consumption().count).isEqualTo(0)
    }

    @Test
    fun `posting a body without a delta returns 400 Bad Request`() {
        val result = postConsumption(emptyMap<String, Any>()).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(400)
        assertThat(result.responseBody!!.errorCode).isEqualTo("BadRequest")
    }

    @Test
    fun `getting the consumption of an unknown user returns 404 Not Found`() {
        val result = getConsumption(UUID.randomUUID()).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `adding a coffee for an unknown user returns 404 Not Found`() {
        val result = postConsumption(ConsumptionDeltaDto(1), UUID.randomUUID()).returnResult<ErrorResponse>()

        assertThat(result.status.value()).isEqualTo(404)
        assertThat(result.responseBody!!.errorCode).isEqualTo("NotFoundException")
    }
}
