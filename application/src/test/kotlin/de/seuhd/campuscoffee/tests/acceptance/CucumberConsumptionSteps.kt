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
 * The scenarios rely on the test data. Before each scenario, [CucumberSpringConfiguration.beforeEach] deletes all data
 * and creates the fixture users of `TestFixtures`, such as `maxmustermann`, each with a coffee count of 0. A login name
 * in a step must be the login name of a fixture user.
 *
 * The steps send their requests through [ConsumptionRequests]. The Given steps remember the user of the scenario and
 * add coffees. The When steps add a coffee or undo the most recent coffee and keep the status code. The Then steps
 * check that status code and the stored coffee count. A step with a time sets the [SettableClock] to that time before
 * its request.
 */
class CucumberConsumptionSteps(
    private val clock: SettableClock
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
        lastStatus = ConsumptionRequests.addCoffee(loginName)
    }

    @Given("the user adds a coffee at {time}")
    fun theUserAddsACoffeeAt(time: LocalTime) {
        clock.setTime(time)
        addCoffeeSuccessfully()
    }

    @When("the user undoes the coffee at {time}")
    fun theUserUndoesTheCoffeeAt(time: LocalTime) {
        clock.setTime(time)
        lastStatus = ConsumptionRequests.undoCoffee(loginName)
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
        assertThat(ConsumptionRequests.coffeeCount(loginName)).isEqualTo(count)
    }

    private fun addCoffeeSuccessfully() {
        assertThat(ConsumptionRequests.addCoffee(loginName)).`as`("status of adding a coffee").isEqualTo(200)
    }
}
