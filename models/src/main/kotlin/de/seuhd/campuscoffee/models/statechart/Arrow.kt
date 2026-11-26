package de.seuhd.campuscoffee.models.statechart

/**
 * An arrow of a chart, which stands for every step between two chart states.
 *
 * @property from the path of the state at the start of the arrow
 * @property to the path of the state at the end of the arrow
 * @property labels the labels of the steps, without duplicates
 * @property panel the title of the panel that draws the arrow, or null for a chart without panels
 */
data class Arrow(
    val from: List<String>,
    val to: List<String>,
    val labels: List<String>,
    val panel: String? = null
)
