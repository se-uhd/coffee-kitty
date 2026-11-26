package de.seuhd.campuscoffee.models.statechart

/**
 * One state of a path that TLC printed.
 *
 * @property action the action that led to [state], or null for the initial state
 * @property state the state after the action
 */
data class TraceStep(
    val action: String?,
    val state: ModelState
)
