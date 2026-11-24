package de.seuhd.campuscoffee.tests.acceptance

import de.seuhd.campuscoffee.tests.ConsumptionRequests
import io.cucumber.java.DataTableType
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalTime

/**
 * Step definitions for the scenarios of FR-8 in `correction.feature`. The steps send their requests through
 * [ConsumptionRequests] and name the users by login name. [CucumberConsumptionSteps] defines the Given steps for the
 * coffees of a single user, the steps of the undo, and the parameter type `{time}`.
 */
class CucumberCorrectionSteps(
    private val clock: SettableClock,
    private val requests: ConsumptionRequests
) {
    /** One correction in a table of a scenario, without the time of the correction. */
    data class Correction(
        val countBefore: Int,
        val countAfter: Int,
        val note: String,
        val adminLoginName: String
    )

    private var lastStatus: Int = 0

    /** Converts "without a note" into null and "with the note "..."" into the text of the note. */
    @ParameterType("without a note|with the note \"([^\"]*)\"")
    fun note(text: String?): String? = text

    /** Converts a row of a table of corrections into a [Correction]. */
    @DataTableType
    fun correction(row: Map<String, String>): Correction =
        Correction(
            countBefore = row.getValue("countBefore").toInt(),
            countAfter = row.getValue("countAfter").toInt(),
            note = row.getValue("note"),
            adminLoginName = row.getValue("adminLoginName")
        )

    @When("{string} corrects the coffee count of {string} to {int} {note}")
    fun correctsTheCoffeeCount(
        actingUser: String,
        loginName: String,
        count: Int,
        note: String?
    ) {
        lastStatus = requests.correctCount(requests.userId(loginName), count, note, actingUser).status
    }

    @Given("{string} corrected the coffee count of {string} to {int} {note} at {time}")
    fun correctedTheCoffeeCountAt(
        actingUser: String,
        loginName: String,
        count: Int,
        note: String?,
        time: LocalTime
    ) {
        clock.setTime(time)
        requests.correctCount(requests.userId(loginName), count, note, actingUser).requireStatus(200)
    }

    @Then("the correction returns the status {int}")
    fun theCorrectionReturnsTheStatus(status: Int) {
        assertThat(lastStatus).isEqualTo(status)
    }

    @Then("the coffee count of {string} is {int}")
    fun theCoffeeCountOfIs(
        loginName: String,
        count: Int
    ) {
        assertThat(requests.retrieveConsumption(requests.userId(loginName)).body.count).isEqualTo(count)
    }

    @Then("the corrections of {string} are:")
    fun theCorrectionsOfAre(
        loginName: String,
        expected: List<Correction>
    ) {
        assertThat(corrections(loginName)).containsExactlyElementsOf(expected)
    }

    @Then("{string} has no corrections")
    fun hasNoCorrections(loginName: String) {
        assertThat(corrections(loginName)).isEmpty()
    }

    /** Returns the corrections of the coffee count of the user with [loginName], newest first, as an admin reads them. */
    private fun corrections(loginName: String): List<Correction> =
        requests
            .retrieveCorrections(requests.userId(loginName))
            .body
            .map { Correction(it.countBefore, it.countAfter, it.note, it.adminLoginName) }
}
