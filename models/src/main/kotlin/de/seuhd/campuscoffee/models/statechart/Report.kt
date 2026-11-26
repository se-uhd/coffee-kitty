package de.seuhd.campuscoffee.models.statechart

/**
 * Writes the report of a model: the size of the model and of the chart, whether each rule holds, the shortest path to
 * each broken rule in the words of the chart, and the steps that the chart does not draw.
 *
 * @param name the name of the model
 * @param view the words of the chart
 * @param result the result of TLC
 * @param chart the chart
 * @return the report as plain text
 */
fun report(
    name: String,
    view: ChartView,
    result: CheckResult,
    chart: Chart
): String =
    buildString {
        val graph = result.graph
        val runs = result.rules.size + 1
        appendLine("Model $name: ${graph.states.size} states and ${graph.steps.size} steps, $runs runs of TLC.")
        appendLine("Chart: ${chart.states.size} states and ${chart.arrows.size} arrows.")
        appendLine()
        appendLine("Rules:")
        val broken = result.broken.map { it.name }.toSet()
        for (rule in result.rules + listOfNotNull(BrokenRule.DEADLOCK.takeIf { it in broken })) {
            val verdict = if (rule in broken) "breaks" else "holds "
            appendLine("  $verdict $rule: ${view.rules[rule] ?: "(the view has no sentence for this rule)"}")
        }
        for (rule in result.broken) {
            appendLine()
            appendLine("Shortest path to a state that breaks ${rule.name}:")
            rule.shortestPath.forEachIndexed { index, step ->
                val before = rule.shortestPath.getOrNull(index - 1)?.state
                val arrow = before?.let { "${view.step(step.action.orEmpty(), it, step.state) ?: step.action} -> " }
                appendLine("  ${index + 1}. ${arrow.orEmpty()}${view.state(step.state).joinToString(" / ")}")
            }
        }
        if (chart.inside.isNotEmpty()) {
            appendLine()
            appendLine("Steps that stay in one state, which the chart does not draw:")
            chart.inside.forEach { (path, labels) ->
                appendLine("  ${path.joinToString(" / ")}: ${labels.joinToString(", ")}")
            }
        }
        if (chart.hidden.isNotEmpty()) {
            appendLine()
            appendLine("Steps that the view hides: ${chart.hidden.joinToString(", ")}")
        }
    }
