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
 * One state of a path that TLC printed.
 *
 * @property action the action that led to [state], or null for the initial state
 * @property state the state after the action
 */
data class TraceStep(
    val action: String?,
    val state: ModelState
)
