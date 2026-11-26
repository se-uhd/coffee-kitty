package de.seuhd.campuscoffee.models.statechart

import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.readText

private const val BREAKS_STYLE = "#FFECEE;line:E72E3B;line.bold;text:CD2834"
private const val MAY_BREAK_STYLE = "#FFF3E6;line:E8730C;line.bold;text:B85A06"
private const val DOWN = "\u2193"
private const val UP = "\u2191"
private val EMBEDDED_SOURCE = Regex("""<\?plantuml-src (\S+)\?>""")

/**
 * Writes [chart] as a PlantUML state diagram. Smetana, the port of Graphviz dot in PlantUML, lays out the diagram, so
 * the build needs no Graphviz. An arrow to a state that the breadth-first search reached earlier points up, and every
 * other arrow points down. The layout treats an upward arrow as a downward arrow in the opposite direction, so the
 * arrows form no cycle and keep their directions. Each line of a label starts
 * with an arrow character that points in the direction of its arrow, so that a reader can tell apart the labels of two
 * arrows between the same states. A chart with panels draws each panel as a composite state with every state and the
 * arrows of the panel. A legend names the rules of the marked states.
 *
 * @param chart the chart
 * @param rules the sentence of the reader for each rule, by its name
 * @return the PlantUML source
 */
fun toPlantUml(
    chart: Chart,
    rules: Map<String, String>
): String =
    buildString {
        appendLine("@startuml")
        appendLine("!pragma layout smetana")
        appendLine("hide empty description")
        appendLine("title ${chart.title}")
        if (chart.panels.isEmpty()) {
            writeDiagram(chart, chart.arrows, "", "")
        }
        chart.panels.forEachIndexed { index, panel ->
            appendLine("state \"${text(panel)}\" as q$index {")
            writeDiagram(chart, chart.arrows.filter { it.panel == panel }, "q$index", "  ")
            appendLine("}")
        }
        if (chart.arrows.isNotEmpty()) {
            appendLine("caption $DOWN and $UP before a label show whether its arrow points down or up.")
        }
        writeLegend(chart, rules)
        appendLine("@enduml")
    }

/**
 * Writes every state of [chart], the arrows from the start, and [arrows], with ids that start with [idPrefix]. For each
 * arrow of another panel, it writes a hidden edge, so that each panel shows the states in the same order.
 */
private fun StringBuilder.writeDiagram(
    chart: Chart,
    arrows: List<Arrow>,
    idPrefix: String,
    indent: String
) {
    val ids = chart.states.withIndex().associate { (index, state) -> state.path to "${idPrefix}s$index" }
    writeStates(chart.states, ids, emptyList(), indent, StateIds(idPrefix, generateSequence(0) { it + 1 }.iterator()))
    chart.initial.forEach { appendLine("$indent[*] --> ${ids.getValue(it)}") }
    val rank = chart.states.withIndex().associate { (index, state) -> state.path to index }
    for (arrow in arrows) {
        val up = rank.getValue(arrow.to) < rank.getValue(arrow.from)
        val label = arrow.labels.joinToString("\\n") { "${if (up) UP else DOWN} ${text(it)}" }
        val line = "${ids.getValue(arrow.from)} ${if (up) "-up->" else "-->"} ${ids.getValue(arrow.to)} : $label"
        appendLine("$indent$line")
    }
    val drawn = arrows.map { downward(it, rank) }.toSet()
    val others =
        chart.arrows
            .filter { it !in arrows }
            .map { downward(it, rank) }
            .distinct() - drawn
    others.forEach { (upper, lower) -> appendLine("$indent${ids.getValue(upper)} -[hidden]-> ${ids.getValue(lower)}") }
}

/** Returns the states of [arrow] with the upper one first, in the order of [rank]. */
private fun downward(
    arrow: Arrow,
    rank: Map<List<String>, Int>
): Pair<List<String>, List<String>> =
    if (rank.getValue(arrow.to) < rank.getValue(arrow.from)) arrow.to to arrow.from else arrow.from to arrow.to

/**
 * The ids of the parent states of one diagram.
 *
 * @property prefix the prefix of the ids, such as the id of a panel
 * @property numbers the numbers of the parent states, in the order in which they are written
 */
private class StateIds(
    val prefix: String,
    val numbers: Iterator<Int>
) {
    /** Returns the id of the next parent state. */
    fun next() = "${prefix}p${numbers.next()}"
}

/** Writes the states below the parents [prefix], and a composite state for each parent one level further down. */
private fun StringBuilder.writeStates(
    states: List<ChartState>,
    ids: Map<List<String>, String>,
    prefix: List<String>,
    indent: String,
    parentIds: StateIds
) {
    val below = states.filter { it.path.size > prefix.size && it.path.subList(0, prefix.size) == prefix }
    for (name in below.map { it.path[prefix.size] }.distinct()) {
        val leaf = below.find { it.path == prefix + name }
        if (leaf != null) {
            require(below.none { it.path.size > prefix.size + 1 && it.path[prefix.size] == name }) {
                "The label ${leaf.path} names both a state and a parent state."
            }
            val style = mapOf(Mark.BREAKS to " $BREAKS_STYLE", Mark.MAY_BREAK to " $MAY_BREAK_STYLE")[leaf.mark] ?: ""
            appendLine("${indent}state \"${text(name)}\" as ${ids.getValue(leaf.path)}$style")
        } else {
            appendLine("${indent}state \"${text(name)}\" as ${parentIds.next()} {")
            writeStates(below, ids, prefix + name, "$indent  ", parentIds)
            appendLine("$indent}")
        }
    }
}

/** Writes a legend with the rule that each marked state breaks, when a state breaks a rule. */
private fun StringBuilder.writeLegend(
    chart: Chart,
    rules: Map<String, String>
) {
    val marked = chart.states.filter { it.mark != Mark.NONE }
    if (marked.isEmpty()) return
    appendLine("legend right")
    appendLine("|= State |= Breaks the rule |")
    for (state in marked) {
        val sentences = state.rules.joinToString("\\n") { text(rules[it] ?: it) }
        val prefix = if (state.mark == Mark.MAY_BREAK) "in some cases: " else ""
        appendLine("| ${text(state.path.joinToString(" / "))} | $prefix$sentences |")
    }
    appendLine("endlegend")
}

/** Replaces the characters that would end a name, a label, or a table cell of PlantUML. */
private fun text(value: String): String = value.replace('"', '\'').replace('|', '/').replace("\n", " ")

/**
 * Renders the PlantUML source [source] as SVG.
 *
 * @param source a PlantUML diagram
 * @return the SVG document
 */
fun renderSvg(source: String): String {
    val out = ByteArrayOutputStream()
    SourceStringReader(source).outputImage(out, FileFormatOption(FileFormat.SVG))
    return out.toString(Charsets.UTF_8)
}

/**
 * Returns whether [file] holds [content]. For an SVG, it compares only the PlantUML source that each SVG embeds,
 * because PlantUML measures text with the fonts of the platform, so that the SVG of one source differs between
 * platforms.
 *
 * @param file a diagram in `doc/`
 * @param content the PlantUML source or the SVG that the model gives now
 */
fun isCurrent(
    file: Path,
    content: String
): Boolean =
    when {
        !file.exists() -> false
        file.extension != "svg" -> file.readText() == content
        else -> embeddedSource(file.readText()) == checkNotNull(embeddedSource(content)) { "The SVG embeds no source." }
    }

/** Returns the PlantUML source that [svg] embeds, in the encoded form of PlantUML, or null when it embeds none. */
private fun embeddedSource(svg: String): String? = EMBEDDED_SOURCE.find(svg)?.groupValues?.get(1)
