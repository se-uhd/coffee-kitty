package de.seuhd.campuscoffee.models.statechart

import de.seuhd.campuscoffee.models.statechart.views.ConsumptionView
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/** The view of each model in `src/main/tla`, by the name of the model. */
val VIEWS: Map<String, ChartView> = mapOf("Consumption" to ConsumptionView)

private const val ARGUMENTS = 4

/**
 * The directories of the command.
 *
 * @property models the directory of the TLA+ modules and their configurations
 * @property charts the directory of the charts
 * @property work the directory for the files of TLC and the reports
 */
private class Directories(
    val models: Path,
    val charts: Path,
    val work: Path
)

/**
 * Checks each model of [VIEWS] with TLC and writes its report. The mode `write` writes the chart of each model as
 * PlantUML source and as SVG. The mode `check` fails when a rule breaks or when a chart differs from its model.
 *
 * @param args the mode (`write` or `check`) and the directories of the models, the charts, and the files of TLC
 */
fun main(args: Array<String>) {
    require(args.size == ARGUMENTS && args[0] in setOf("write", "check")) {
        "Usage: statechart <write|check> <model directory> <chart directory> <work directory>"
    }
    val (models, charts, work) = args.drop(1).map { Path.of(it) }
    val tlc = Tlc(javaCommand(), tla2tools())
    val problems =
        VIEWS.flatMap { (name, view) ->
            process(args[0], name, view, Directories(models, charts, work), tlc)
        }
    if (problems.isNotEmpty()) {
        System.err.println(problems.joinToString("\n"))
        exitProcess(1)
    }
}

private fun process(
    mode: String,
    name: String,
    view: ChartView,
    directories: Directories,
    tlc: Tlc
): List<String> {
    val work = directories.work.resolve(name)
    work.toFile().deleteRecursively()
    val result = tlc.check(directories.models.resolve("$name.tla"), directories.models.resolve("$name.cfg"), work)
    val chart = buildChart(view, result.graph, result.broken)
    val source = toPlantUml(chart, view.rules)
    val reportFile = work.resolve("report.txt")
    reportFile.writeText(report(name, view, result, chart))
    println(reportFile.readText())
    val files =
        mapOf(
            directories.charts.resolve("$name.puml") to source,
            directories.charts.resolve("$name.svg") to renderSvg(source)
        )
    if (mode == "write") {
        directories.charts.createDirectories()
        files.forEach { (file, content) -> file.writeText(content) }
        return emptyList()
    }
    val stale = files.filterNot { (file, content) -> isCurrent(file, content) }.keys
    return stale.map { "$it differs from the model $name. Run `gradle :models:statecharts` and commit it." } +
        result.broken.map { "The model $name breaks the rule ${it.name}. See $reportFile." }
}

/** Returns the `java` command of the running JVM, which also runs TLC. */
internal fun javaCommand(): String =
    ProcessHandle
        .current()
        .info()
        .command()
        .orElse("java")

/**
 * Returns the path of tla2tools.jar: the environment variable `TLA2TOOLS`, or else the jar that mise installed for the
 * tool `http:tla2tools` of mise.toml.
 */
internal fun tla2tools(): Path {
    System.getenv("TLA2TOOLS")?.let { return Path.of(it) }
    val help = "Run `mise install` in the repository, or set TLA2TOOLS to the path of tla2tools.jar."
    val command = ProcessBuilder("mise", "where", "http:tla2tools").redirectError(ProcessBuilder.Redirect.INHERIT)
    val failure = "Cannot run mise to find tla2tools.jar."
    val process = runCatching { command.start() }.getOrElse { error("$failure $help ($it)") }
    val directory =
        process.inputStream
            .bufferedReader()
            .readLines()
            .lastOrNull { it.isNotBlank() }
            ?.trim()
    check(process.waitFor() == 0) { "Cannot find tla2tools.jar. $help" }
    return Path.of(checkNotNull(directory) { "mise printed no directory for tla2tools.jar. $help" }, "tla2tools.jar")
}
