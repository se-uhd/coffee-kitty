package de.seuhd.campuscoffee.models.statechart

/**
 * The state graph that TLC writes with `-dump dot,actionlabels`.
 *
 * @property states each state, by the id that TLC gives it
 * @property initial the ids of the initial states
 * @property steps each step from one state to another
 */
class StateGraph(
    val states: Map<String, ModelState>,
    val initial: Set<String>,
    val steps: List<Step>
)

private val EDGE = Regex("""^(-?\d+) -> (-?\d+) \[label="([^"]*)"""")

private val NODE = Regex("""^(-?\d+) \[label="((?:[^"\\]|\\.)*)"(,style = filled)?""")

/**
 * Parses the DOT file that TLC writes with `-dump dot,actionlabels`. TLC fills the node of each initial state.
 *
 * @param dot the content of the DOT file
 * @return the states and the steps of the file
 */
fun parseDot(dot: String): StateGraph {
    val states = linkedMapOf<String, ModelState>()
    val initial = linkedSetOf<String>()
    val steps = mutableListOf<Step>()
    for (line in dot.lines()) {
        val edge = EDGE.find(line)
        val node = NODE.find(line)
        if (edge != null) {
            val (from, to, action) = edge.destructured
            steps += Step(from, to, action)
        } else if (node != null) {
            val (id, label, filled) = node.destructured
            states[id] = parseModelState(unescapeDot(label))
            if (filled.isNotEmpty()) initial += id
        }
    }
    require(steps.all { it.from in states && it.to in states }) {
        "The DOT file has a step to or from a state that it does not list."
    }
    return StateGraph(states, initial, steps.distinct())
}

private fun unescapeDot(label: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < label.length) {
        if (label[i] == '\\' && i + 1 < label.length) {
            i++
            out.append(if (label[i] == 'n') '\n' else label[i])
        } else {
            out.append(label[i])
        }
        i++
    }
    return out.toString()
}
