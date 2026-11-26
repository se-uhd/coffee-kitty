package de.seuhd.campuscoffee.models.statechart

/**
 * A state of a chart, which stands for every model state with the same label.
 *
 * @property path the label of the state, after the labels of its parent states
 * @property mark [Mark.BREAKS] when every model state behind it breaks a rule, and [Mark.MAY_BREAK] when some do
 * @property rules the rules that a model state behind it breaks
 */
data class ChartState(
    val path: List<String>,
    val mark: Mark,
    val rules: Set<String>
)
