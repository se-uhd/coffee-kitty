package de.seuhd.campuscoffee.models.statechart.views

import de.seuhd.campuscoffee.models.statechart.ChartView
import de.seuhd.campuscoffee.models.statechart.ModelState
import de.seuhd.campuscoffee.models.statechart.int
import de.seuhd.campuscoffee.models.statechart.items
import de.seuhd.campuscoffee.models.statechart.string

/** The words of the chart of `Consumption.tla`: the coffee count of a user. */
object ConsumptionView : ChartView {
    override val title = "Coffee count of a user (FR-3, FR-8, FR-13)"

    override val rules =
        mapOf(
            "CountNeverNegative" to "The coffee count is never negative.",
            "NoMoreTimesThanCoffees" to "The app never stores more coffee times than the coffee count."
        )

    override val panels =
        mapOf(
            "A user adds and undoes coffees" to setOf("AddCoffee", "Undo", "GracePeriodEnds"),
            "An admin corrects the coffee count of a user" to setOf("Correct")
        )

    override fun state(state: ModelState): List<String> =
        when {
            state["count"].int == 0L -> {
                listOf("The coffee count is 0")
            }

            state["times"].items.lastOrNull()?.string == "recent" -> {
                listOf(
                    "The coffee count is above 0",
                    "A user can undo a coffee"
                )
            }

            else -> {
                listOf("The coffee count is above 0", "A user cannot undo a coffee")
            }
        }

    override fun step(
        action: String,
        from: ModelState,
        to: ModelState
    ): String =
        when (action) {
            "AddCoffee" -> {
                "a user adds a coffee"
            }

            "RefuseCoffee" -> {
                "the app refuses a coffee at the largest coffee count that it can store"
            }

            "Undo" -> {
                "a user undoes their most recent coffee"
            }

            "Correct" -> {
                "an admin corrects the coffee count of a user"
            }

            "RefuseDelta" -> {
                "the app refuses a request with a delta other than +1"
            }

            "RefuseCorrection" -> {
                "the app refuses a correction to a negative count or with a note that is blank, longer than 500 " +
                    "characters, or contains the NUL character"
            }

            "RefuseCorrectionByUser" -> {
                "the app refuses a correction by a user who is not an admin"
            }

            "GracePeriodEnds" -> {
                "the grace period of a coffee ends"
            }

            else -> {
                action
            }
        }
}
