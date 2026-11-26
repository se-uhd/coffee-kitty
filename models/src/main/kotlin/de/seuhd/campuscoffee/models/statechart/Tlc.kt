package de.seuhd.campuscoffee.models.statechart

import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Runs TLC, the model checker of TLA+, in a separate JVM.
 *
 * @property java the `java` command of the JVM for TLC
 * @property jar the path of tla2tools.jar
 */
class Tlc(
    private val java: String,
    private val jar: Path
) {
    /**
     * Checks the model [spec] with the configuration [config] in one run for the state graph and one run for each
     * INVARIANT. Each run works on a copy of the TLA+ modules next to [spec], because TLC reads the configuration only
     * from the directory of the module and writes trace files there.
     *
     * @param spec the TLA+ module of the model
     * @param config the TLC configuration of the model
     * @param workDir the directory for the files of each run
     * @return the state graph and the broken rules
     */
    fun check(
        spec: Path,
        config: Path,
        workDir: Path
    ): CheckResult {
        val tlcConfig = TlcConfig.parse(config.readText())
        val graphDir = prepare(spec, workDir.resolve("graph"), tlcConfig.withInvariants(emptyList()))
        run(spec, graphDir, dump = true)
        val graph = parseDot(graphDir.resolve("graph.dot").readText())
        val broken =
            tlcConfig.invariants.mapNotNull { rule ->
                val dir = prepare(spec, workDir.resolve("rule-$rule"), tlcConfig.withInvariants(listOf(rule)))
                val found = parseViolations(run(spec, dir, dump = false))
                check(found.all { it.name == rule }) { "TLC reported a rule other than $rule. See $dir." }
                found.singleOrNull()
            }
        val deadlock = if (tlcConfig.checksDeadlock) deadlock(graph) else null
        return CheckResult(graph, tlcConfig.invariants, broken + listOfNotNull(deadlock))
    }

    private fun prepare(
        spec: Path,
        dir: Path,
        config: String
    ): Path {
        dir.createDirectories()
        spec.parent.listDirectoryEntries("*.tla").forEach { it.copyTo(dir.resolve(it.name)) }
        dir.resolve("${spec.nameWithoutExtension}.cfg").writeText(config)
        return dir
    }

    private fun run(
        spec: Path,
        dir: Path,
        dump: Boolean
    ): String {
        // External: https://lamport.azurewebsites.net/tla/current-tools.pdf -deadlock turns off the deadlock check.
        val command =
            listOf(java, "-XX:+UseParallelGC", "-cp", jar.toString(), "tlc2.TLC", "-workers", "1", "-continue") +
                listOf("-deadlock", "-config", "${spec.nameWithoutExtension}.cfg", "-metadir", "states") +
                (if (dump) listOf("-dump", "dot,actionlabels", "graph") else emptyList()) +
                spec.name
        val process = ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        val log = dir.resolve("tlc.out")
        log.writeText(output)
        val unexpected =
            output.lines().filter {
                it.startsWith("Error:") &&
                    KNOWN_ERRORS.none { known -> known.containsMatchIn(it) }
            }
        check("Model checking completed" in output && unexpected.isEmpty()) {
            "TLC stopped with exit code $exit or printed an error that is not a broken rule. Its output is in $log:\n" +
                output.lines().takeLast(OUTPUT_TAIL).joinToString("\n")
        }
        return output
    }

    private companion object {
        const val OUTPUT_TAIL = 30
    }
}

/**
 * Returns the deadlock of [graph]: each state without a step out, with the shortest path to such a state, or null when
 * each state has a step out. A step that leaves the state as it is counts as a step out.
 *
 * @param graph a complete state graph
 * @return the broken rule [BrokenRule.DEADLOCK], or null
 */
fun deadlock(graph: StateGraph): BrokenRule? {
    val stuck = graph.states.keys - graph.steps.map { it.from }.toSet()
    if (stuck.isEmpty()) return null
    val reachedBy = mutableMapOf<String, Step?>()
    graph.initial.forEach { reachedBy[it] = null }
    val queue = ArrayDeque(graph.initial)
    var target: String? = null
    while (queue.isNotEmpty() && target == null) {
        val id = queue.removeFirst()
        if (id in stuck) target = id
        graph.steps.filter { it.from == id && it.to !in reachedBy }.forEach {
            reachedBy[it.to] = it
            queue += it.to
        }
    }
    val path = generateSequence(target) { reachedBy[it]?.from }.toList().reversed()
    return BrokenRule(
        BrokenRule.DEADLOCK,
        stuck.map { graph.states.getValue(it) }.toSet(),
        path.map { TraceStep(reachedBy[it]?.action, graph.states.getValue(it)) }
    )
}

private const val BEHAVIOR_LINE = "Error: The behavior up to this point is:"

private val VIOLATION = Regex("""^Error: Invariant (\S+) is violated\.$""")

private val INITIAL_VIOLATION = Regex("""^Error: Invariant (\S+) is violated by the initial state:$""")

private val STATE_HEADER = Regex("""^State \d+: <(\S+)""")

private val KNOWN_ERRORS = listOf(VIOLATION, INITIAL_VIOLATION, Regex("^${Regex.escape(BEHAVIOR_LINE)}$"))

/**
 * Reads each rule that TLC reports as broken, with the path that TLC prints for each state that breaks it. TLC prints
 * an initial state that breaks a rule without a path.
 *
 * @param output the output of one run of TLC
 * @return the broken rules, in the order in which TLC reported them
 */
fun parseViolations(output: String): List<BrokenRule> {
    val lines = output.lines()
    val paths = mutableListOf<Pair<String, List<TraceStep>>>()
    var i = 0
    while (i < lines.size) {
        val violation = VIOLATION.find(lines[i])
        val initial = INITIAL_VIOLATION.find(lines[i])
        i++
        if (violation != null) {
            val (path, next) = readPath(lines, i)
            paths += violation.groupValues[1] to path
            i = next
        } else if (initial != null) {
            val body = lines.subList(i, lines.size).takeWhile { it.isNotBlank() }
            paths += initial.groupValues[1] to listOf(TraceStep(null, parseModelState(body.joinToString("\n"))))
            i += body.size
        }
    }
    return paths.groupBy({ it.first }, { it.second }).map { (rule, rulePaths) ->
        BrokenRule(rule, rulePaths.mapNotNull { it.lastOrNull()?.state }.toSet(), rulePaths.minBy { it.size })
    }
}

private fun readPath(
    lines: List<String>,
    start: Int
): Pair<List<TraceStep>, Int> {
    val path = mutableListOf<TraceStep>()
    var i = if (lines.getOrNull(start) == BEHAVIOR_LINE) start + 1 else start
    var header = lines.getOrNull(i)?.let { STATE_HEADER.find(it) }
    while (header != null) {
        val body = lines.subList(i + 1, lines.size).takeWhile { it.isNotBlank() }
        val action = header.groupValues[1].takeUnless { it == "Initial" }
        path += TraceStep(action, parseModelState(body.joinToString("\n")))
        i += body.size + 2
        header = lines.getOrNull(i)?.let { STATE_HEADER.find(it) }
    }
    return path to i
}
