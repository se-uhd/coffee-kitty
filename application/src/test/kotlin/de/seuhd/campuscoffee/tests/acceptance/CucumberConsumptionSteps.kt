package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.tests.ConsumptionRequests
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalTime

/**
 * Step definitions for the acceptance scenarios in `consumption.feature`.
 *
 * Before each scenario, [CucumberSpringConfiguration.beforeEach] cleans the database and creates the fixture users of
 * `TestFixtures`, such as `maxmustermann`, each with a coffee count of 0. A login name in a step must be the login name
 * of a fixture user.
 *
 * The steps send their requests through [ConsumptionRequests]. The first Given step stores the login name of the user
 * of the scenario. Each When step stores the status code for the Then steps. A step with a time sets the
 * [SettableClock] to that time before its request. The step with a coffee count and the time of the last coffee adds
 * the other coffees one hour before that time.
 */
class CucumberConsumptionSteps(
    private val clock: SettableClock,
    private val requests: ConsumptionRequests
) {
    private lateinit var loginName: String
    private var lastStatus: Int = 0

    /** Converts a time such as 10:05:00 in a step into a [LocalTime] for the parameter type `{time}`. */
    @ParameterType("\\d{2}:\\d{2}:\\d{2}")
    fun time(value: String): LocalTime = LocalTime.parse(value)

    @Given("the user {string}")
    fun theUser(loginName: String) {
        this.loginName = loginName
    }

    @Given("the user {string} has {int} coffees")
    fun theUserHasCoffees(
        loginName: String,
        count: Int
    ) {
        theUser(loginName)
        repeat(count) { addCoffeeSuccessfully() }
        theCoffeeCountIs(count)
    }

    @Given("the user {string} has {int} coffees, the last one added at {time}")
    fun theUserHasCoffeesTheLastOneAddedAt(
        loginName: String,
        count: Int,
        lastAddedAt: LocalTime
    ) {
        require(count >= 1) { "The step needs at least one coffee." }
        theUser(loginName)
        clock.setTime(lastAddedAt.minusHours(1))
        repeat(count - 1) { addCoffeeSuccessfully() }
        clock.setTime(lastAddedAt)
        addCoffeeSuccessfully()
    }

    @When("the user adds a coffee")
    fun theUserAddsACoffee() {
        lastStatus = requests.addCoffee(requests.userId(loginName)).status
    }

    @Given("the user adds a coffee at {time}")
    fun theUserAddsACoffeeAt(time: LocalTime) {
        clock.setTime(time)
        addCoffeeSuccessfully()
    }

    @When("the user undoes the coffee at {time}")
    fun theUserUndoesTheCoffeeAt(time: LocalTime) {
        clock.setTime(time)
        lastStatus = requests.undo(requests.userId(loginName)).status
    }

    @Then("the request succeeds and the coffee count is {int}")
    fun theRequestSucceedsAndTheCoffeeCountIs(count: Int) {
        theResponseStatusIs(200)
        theCoffeeCountIs(count)
    }

    @Then("the response status is {int}")
    fun theResponseStatusIs(status: Int) {
        assertThat(lastStatus).isEqualTo(status)
    }

    @Then("the coffee count is {int}")
    fun theCoffeeCountIs(count: Int) {
        assertThat(requests.retrieveConsumption(requests.userId(loginName)).body.count).isEqualTo(count)
    }

    private fun addCoffeeSuccessfully() {
        requests.addCoffee(requests.userId(loginName)).requireStatus(200)
    }
}
