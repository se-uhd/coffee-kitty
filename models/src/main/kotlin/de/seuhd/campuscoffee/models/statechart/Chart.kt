package de.seuhd.campuscoffee.models.statechart

/**
 * The statechart of a model in the words of the reader.
 *
 * @property title the title of the chart
 * @property states the states, in the order of a breadth-first search from the initial states
 * @property initial the paths of the initial states
 * @property arrows the arrows between two different states
 * @property inside the labels of the steps that stay in one chart state, by its path, which the chart does not draw
 * @property hidden the actions of the steps that the view hides
 * @property panels the titles of the panels, or none for a chart without panels
 */
class Chart(
    val title: String,
    val states: List<ChartState>,
    val initial: List<List<String>>,
    val arrows: List<Arrow>,
    val inside: Map<List<String>, Set<String>>,
    val hidden: Set<String>,
    val panels: List<String> = emptyList()
)

/**
 * Builds the chart of [graph] in the words of [view] and marks the states that break a rule. When the view has panels,
 * each arrow belongs to the panel of its action.
 *
 * @param view the labels of the states and the steps
 * @param graph the state graph of the model
 * @param broken the broken rules
 * @return the chart
 */
fun buildChart(
    view: ChartView,
    graph: StateGraph,
    broken: List<BrokenRule>
): Chart {
    val pathOf = graph.states.mapValues { (_, state) -> view.state(state) }
    val labels = linkedMapOf<Triple<List<String>, List<String>, String?>, MutableSet<String>>()
    val hidden = sortedSetOf<String>()
    for ((source, target, action) in graph.steps) {
        val label = view.step(action, graph.states.getValue(source), graph.states.getValue(target))
        val from = pathOf.getValue(source)
        val to = pathOf.getValue(target)
        when {
            label == null -> hidden += action
            from == to -> labels.getOrPut(Triple(from, to, null)) { sortedSetOf() } += label
            else -> labels.getOrPut(Triple(from, to, panelOf(view, action))) { sortedSetOf() } += label
        }
    }
    val initial = graph.initial.map { pathOf.getValue(it) }.distinct()
    val edges = labels.keys.filter { it.first != it.second }.map { it.first to it.second }
    val order = searchOrder(initial, edges, pathOf.values.distinct())
    val arrows =
        labels
            .filterKeys { it.first != it.second }
            .map { (key, stepLabels) -> Arrow(key.first, key.second, stepLabels.toList(), key.third) }
            .sortedWith(compareBy({ order.indexOf(it.from) }, { order.indexOf(it.to) }))
    val inside =
        labels.filterKeys { it.first == it.second }.map { (key, stepLabels) ->
            key.first to stepLabels.toSet()
        }
    val states = order.map { chartState(it, graph, pathOf, broken) }
    return Chart(view.title, states, initial, arrows, inside.toMap(), hidden, view.panels.keys.toList())
}

private fun panelOf(
    view: ChartView,
    action: String
): String? {
    if (view.panels.isEmpty()) return null
    val panels = view.panels.filterValues { action in it }.keys
    require(panels.size == 1) {
        "The action $action has an arrow, so exactly one panel of the view must list it, but ${panels.size} do."
    }
    return panels.single()
}

private fun chartState(
    path: List<String>,
    graph: StateGraph,
    pathOf: Map<String, List<String>>,
    broken: List<BrokenRule>
): ChartState {
    val states = pathOf.filterValues { it == path }.keys.map { graph.states.getValue(it) }
    val rulesOf = states.associateWith { state -> broken.filter { state in it.states }.map { it.name } }
    val breaking = states.count { rulesOf.getValue(it).isNotEmpty() }
    val mark =
        when (breaking) {
            0 -> Mark.NONE
            states.size -> Mark.BREAKS
            else -> Mark.MAY_BREAK
        }
    return ChartState(path, mark, rulesOf.values.flatten().toSortedSet())
}

private fun searchOrder(
    initial: List<List<String>>,
    edges: List<Pair<List<String>, List<String>>>,
    all: List<List<String>>
): List<List<String>> {
    val order = initial.toMutableList()
    var next = 0
    while (next < order.size) {
        val current = order[next++]
        edges.filter { it.first == current && it.second !in order }.forEach { order += it.second }
    }
    return order + all.filter { it !in order }.sortedBy { it.joinToString("/") }
}
