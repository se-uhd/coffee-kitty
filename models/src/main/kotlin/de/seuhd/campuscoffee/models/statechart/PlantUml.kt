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
 * Writes [chart] as a PlantUML state diagram. An arrow to a state that the breadth-first search reached earlier points
 * up, which the layout treats as a downward arrow in the opposite direction, so the arrows form no cycle. Each panel
 * is a composite state, and hidden edges for the arrows of the other panels keep the states in the same order.
 *
 * @param chart the chart
 * @param rules the sentence of the reader for each rule, by its name
 * @param model the name of the TLA+ model, such as `Consumption`, which the caption names as the source of the chart
 * @return the PlantUML source
 */
fun toPlantUml(
    chart: Chart,
    rules: Map<String, String>,
    model: String
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
        val source = "TLC derived the states and steps from the TLA+ model $model.tla."
        val arrows = "\\n$DOWN and $UP before a label show whether its arrow points down or up."
        appendLine("caption $source${if (chart.arrows.isEmpty()) "" else arrows}")
        writeLegend(chart, rules)
        appendLine("@enduml")
    }

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
    for ((from, to, labels) in arrows) {
        val up = rank.getValue(to) < rank.getValue(from)
        val label = labels.joinToString("\\n") { "${if (up) UP else DOWN} ${text(it)}" }
        val line = "${ids.getValue(from)} ${if (up) "-up->" else "-->"} ${ids.getValue(to)} : $label"
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

private fun downward(
    arrow: Arrow,
    rank: Map<List<String>, Int>
): Pair<List<String>, List<String>> =
    if (rank.getValue(arrow.to) < rank.getValue(arrow.from)) arrow.to to arrow.from else arrow.from to arrow.to

private class StateIds(
    val prefix: String,
    val numbers: Iterator<Int>
) {
    /** Returns the id of the next parent state. */
    fun next() = "${prefix}p${numbers.next()}"
}

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
            val styles = mapOf(Mark.BREAKS to " $BREAKS_STYLE", Mark.MAY_BREAK to " $MAY_BREAK_STYLE")
            val style = styles[leaf.mark].orEmpty()
            appendLine("${indent}state \"${text(name)}\" as ${ids.getValue(leaf.path)}$style")
        } else {
            appendLine("${indent}state \"${text(name)}\" as ${parentIds.next()} {")
            writeStates(below, ids, prefix + name, "$indent  ", parentIds)
            appendLine("$indent}")
        }
    }
}

private fun StringBuilder.writeLegend(
    chart: Chart,
    rules: Map<String, String>
) {
    val marked = chart.states.filter { it.mark != Mark.NONE }
    if (marked.isEmpty()) return
    appendLine("legend right")
    appendLine("|= State |= Breaks the rule |")
    for ((path, mark, broken) in marked) {
        val sentences = broken.joinToString("\\n") { text(rules[it] ?: it) }
        val prefix = if (mark == Mark.MAY_BREAK) "in some cases: " else ""
        appendLine("| ${text(path.joinToString(" / "))} | $prefix$sentences |")
    }
    appendLine("endlegend")
}

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

private fun embeddedSource(svg: String): String? = EMBEDDED_SOURCE.find(svg)?.groupValues?.get(1)
