package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.EntityExchangeResult
import org.springframework.test.web.servlet.client.returnResult
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Step definitions for the coffee consumption acceptance scenarios. The Given step looks up the user's id in the
 * user list (`GET /api/users`), and the When step records the latest response, so that the Then step can assert the
 * status and the returned count. Adding a coffee posts a delta of `+1` to `/api/users/{userId}/consumption`, which
 * returns the user's login name and coffee count.
 */
class CucumberConsumptionSteps(
    private val objectMapper: ObjectMapper
) {
    private lateinit var userId: UUID
    private lateinit var lastResult: EntityExchangeResult<ByteArray>

    @Given("the user {string}")
    fun theUser(loginName: String) {
        val users =
            client()
                .get()
                .uri("/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<Array<UserDto>>()
                .responseBody!!
        userId = users.single { it.loginName == loginName }.persistedId
    }

    @When("the user adds a coffee")
    fun theUserAddsACoffee() {
        lastResult =
            client()
                .post()
                .uri("/api/users/{userId}/consumption", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ConsumptionDeltaDto(1))
                .exchange()
                .returnResult<ByteArray>()
    }

    @Then("the request succeeds and the coffee count is {int}")
    fun theRequestSucceedsAndTheCoffeeCountIs(count: Int) {
        assertThat(lastResult.status.value()).isEqualTo(200)
        val dto = objectMapper.readValue(lastResult.responseBody, ConsumptionDto::class.java)
        assertThat(dto.count).isEqualTo(count)
    }
}
