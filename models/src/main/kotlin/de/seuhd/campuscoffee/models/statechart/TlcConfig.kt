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
 * A TLC configuration file, split into its INVARIANTs and the other tokens.
 *
 * @property invariants the names after INVARIANT or INVARIANTS
 * @property rest the other tokens of the file, in their order
 */
class TlcConfig(
    val invariants: List<String>,
    private val rest: List<String>
) {
    /** Whether a state without a step out breaks a rule, which `CHECK_DEADLOCK FALSE` turns off. */
    val checksDeadlock: Boolean =
        rest.indexOf("CHECK_DEADLOCK").let { index -> index < 0 || rest.getOrNull(index + 1) != "FALSE" }

    /**
     * Returns the text of the configuration with only the INVARIANTs [keep].
     *
     * @param keep the INVARIANTs for TLC to check
     */
    fun withInvariants(keep: List<String>): String =
        buildString {
            appendLine(rest.joinToString(" "))
            if (keep.isNotEmpty()) appendLine("INVARIANTS ${keep.joinToString(" ")}")
        }

    companion object {
        private val KEYWORDS =
            setOf(
                "CONSTANT",
                "CONSTANTS",
                "INIT",
                "NEXT",
                "SPECIFICATION",
                "INVARIANT",
                "INVARIANTS",
                "PROPERTY",
                "PROPERTIES",
                "CONSTRAINT",
                "CONSTRAINTS",
                "ACTION_CONSTRAINT",
                "ACTION_CONSTRAINTS",
                "SYMMETRY",
                "VIEW",
                "CHECK_DEADLOCK",
                "POSTCONDITION",
                "ALIAS"
            )

        /**
         * The keywords that the command does not support. With a CONSTRAINT, TLC leaves out the steps from the states
         * at the bound, which would then count as deadlocks, and a PROPERTY needs the check of a temporal formula.
         */
        private val UNSUPPORTED =
            setOf("CONSTRAINT", "CONSTRAINTS", "ACTION_CONSTRAINT", "ACTION_CONSTRAINTS", "PROPERTY", "PROPERTIES")

        /**
         * Parses a TLC configuration file. A `\*` starts a comment that ends at the end of the line.
         *
         * @param text the content of the file
         * @return the INVARIANTs and the other tokens of the file
         * @throws IllegalArgumentException if the file uses a keyword that the command does not support
         */
        fun parse(text: String): TlcConfig {
            val tokens =
                text
                    .lines()
                    .flatMap {
                        it
                            .substringBefore(
                                "\\*"
                            ).split(Regex("\\s+"))
                    }.filter { it.isNotEmpty() }
            val unsupported = tokens.filter { it in UNSUPPORTED }.distinct()
            require(
                unsupported.isEmpty()
            ) { "The command does not support ${unsupported.joinToString()} in a TLC configuration." }
            val invariants = mutableListOf<String>()
            val rest = mutableListOf<String>()
            var inInvariants = false
            for (token in tokens) {
                if (token in KEYWORDS) inInvariants = token.startsWith("INVARIANT")
                when {
                    token.startsWith("INVARIANT") && token in KEYWORDS -> Unit
                    inInvariants -> invariants += token
                    else -> rest += token
                }
            }
            return TlcConfig(invariants, rest)
        }
    }
}
