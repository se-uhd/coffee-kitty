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
 * The result of checking a model with TLC.
 *
 * @property graph the complete state graph
 * @property rules the INVARIANTs of the configuration
 * @property broken the rules that break, in the order of the configuration, and then a deadlock
 */
class CheckResult(
    val graph: StateGraph,
    val rules: List<String>,
    val broken: List<BrokenRule>
)
