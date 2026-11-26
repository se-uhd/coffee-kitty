package de.seuhd.campuscoffee

import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * The limits of one group of Markdown files: one path or a glob. The first entry that matches a file applies.
 *
 * @property adr the settings of [AdrStructure], or null for a file that is not an ADR
 * @property source where the limit comes from, which the message of a violation repeats
 */
data class MarkdownEntry(
    val path: String?,
    val glob: String?,
    val maxLines: Int?,
    val maxSectionLines: Int?,
    val includeUnscopedRules: Boolean,
    val changelog: Boolean,
    val maxBulletLines: Int?,
    val adr: Map<String, Any?>?,
    val source: String
) {
    private val matcher = glob?.let { FileSystems.getDefault().getPathMatcher("glob:$it") }

    fun matches(path: String): Boolean = path == this.path || matcher?.matches(Path.of(path)) == true
}
