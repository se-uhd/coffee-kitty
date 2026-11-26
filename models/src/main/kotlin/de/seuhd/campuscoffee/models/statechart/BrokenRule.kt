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
 * A rule that TLC found broken: an INVARIANT of the model, or [DEADLOCK].
 *
 * @property name the name of the rule
 * @property states each state that breaks the rule
 * @property shortestPath the shortest path from an initial state to a state that breaks the rule
 */
class BrokenRule(
    val name: String,
    val states: Set<ModelState>,
    val shortestPath: List<TraceStep>
) {
    companion object {
        /** The name of the rule that a state without a step out breaks. */
        const val DEADLOCK = "NoDeadlock"
    }
}
