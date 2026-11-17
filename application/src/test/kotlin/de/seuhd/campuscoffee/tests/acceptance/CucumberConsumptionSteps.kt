package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.SystemTestUtils.requireStatus
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.EntityExchangeResult
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Step definitions for the acceptance scenarios in `consumption.feature`.
 *
 * The scenarios rely on the test data. Before each scenario, [CucumberSpringConfiguration.beforeEach] deletes all data
 * and creates the fixture users of `TestFixtures`, such as `maxmustermann`, each with a coffee count of 0. A login name
 * in a step must be the login name of a fixture user.
 *
 * The Given steps find the id of the user in the list from `GET /api/users`, add coffees through
 * `POST /api/users/{userId}/consumption`, and check the resulting coffee count. The When step adds a coffee and keeps
 * the response. The Then steps check that response or read the coffee count with `GET /api/users/{userId}/consumption`.
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

    @Given("the user {string} has {int} coffees")
    fun theUserHasCoffees(
        loginName: String,
        count: Int
    ) {
        theUser(loginName)
        repeat(count) { postCoffee().requireStatus(200) }
        theCoffeeCountIs(count)
    }

    @When("the user adds a coffee")
    fun theUserAddsACoffee() {
        lastResult = postCoffee().returnResult<ByteArray>()
    }

    @Then("the request succeeds and the coffee count is {int}")
    fun theRequestSucceedsAndTheCoffeeCountIs(count: Int) {
        assertThat(lastResult.status.value()).isEqualTo(200)
        val dto = objectMapper.readValue(lastResult.responseBody, ConsumptionDto::class.java)
        assertThat(dto.count).isEqualTo(count)
    }

    @Then("the coffee count is {int}")
    fun theCoffeeCountIs(count: Int) {
        val consumption =
            client()
                .get()
                .uri("/api/users/{userId}/consumption", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult<ConsumptionDto>()
                .responseBody!!
        assertThat(consumption.count).isEqualTo(count)
    }

    private fun postCoffee(): RestTestClient.ResponseSpec =
        client()
            .post()
            .uri("/api/users/{userId}/consumption", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ConsumptionDeltaDto(1))
            .exchange()
}
