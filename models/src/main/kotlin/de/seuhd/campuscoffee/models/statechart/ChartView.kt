package de.seuhd.campuscoffee.models.statechart

/**
 * The labels of the chart of one model, in the words of its reader, such as a stakeholder of a requirement. Each model
 * with a chart has one view, which [VIEWS] lists.
 */
interface ChartView {
    /** The title at the top of the chart. */
    val title: String

    /** The sentence of the reader for each rule of the model, by the name of its INVARIANT. */
    val rules: Map<String, String>

    /**
     * Returns the label of the chart state of [state], after the labels of its parent states. The model states with
     * the same label form one chart state.
     *
     * @param state a state of the model
     */
    fun state(state: ModelState): List<String>

    /**
     * Returns the label of the arrow for a step, in the words of the reader of the chart, or null to hide the step.
     *
     * @param action the name of the TLA+ action that takes the step
     * @param from the state before the step
     * @param to the state after the step
     */
    fun step(
        action: String,
        from: ModelState,
        to: ModelState
    ): String?

    /**
     * The panels of the chart by their title, each with the actions whose arrows it draws, such as one panel for the
     * steps of a user and one for the steps of an admin. Each panel draws every state. Without panels, the chart is
     * one diagram with every arrow.
     */
    val panels: Map<String, Set<String>> get() = emptyMap()
}
