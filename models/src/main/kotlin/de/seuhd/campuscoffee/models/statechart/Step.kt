package de.seuhd.campuscoffee.models.statechart

/**
 * One step of a state graph.
 *
 * @property from the id of the state before the step
 * @property to the id of the state after the step
 * @property action the name of the TLA+ action that takes the step
 */
data class Step(
    val from: String,
    val to: String,
    val action: String
)
