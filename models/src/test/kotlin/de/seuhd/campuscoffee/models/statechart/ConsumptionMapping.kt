package de.seuhd.campuscoffee.models.statechart

import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService

/** The link between the models of the consumption and [CoffeeConsumptionService]. */
internal object ConsumptionMapping {
    /** The function of [CoffeeConsumptionService] that each action of `Consumption.tla` calls. */
    val FUNCTIONS =
        mapOf(
            "AddCoffee" to "applyDelta",
            "RefuseCoffee" to "applyDelta",
            "RefuseDelta" to "applyDelta",
            "Undo" to "undo",
            "Correct" to "correctCount",
            "RefuseCorrection" to "correctCount",
            "RefuseStaleCorrection" to "correctCount",
            "RefuseCorrectionByUser" to "correctCount"
        )

    /** The functions of [CoffeeConsumptionService] that change nothing. */
    val QUERIES = setOf("getByUserId", "undoableUntil", "getCorrections")
}
