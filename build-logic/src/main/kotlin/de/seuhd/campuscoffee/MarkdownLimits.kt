package de.seuhd.campuscoffee

import org.yaml.snakeyaml.Yaml
import java.io.File

/** The limits of `config/markdown/limits.yml`. */
class MarkdownLimits(
    private val lineLength: Int,
    private val entries: List<MarkdownEntry>
) {
    /** @param files each Markdown file by its path relative to the root of the repository */
    fun check(files: Map<String, File>): List<String> {
        val adrs = files.keys.filter { path -> entries.firstOrNull { it.matches(path) }?.adr != null }
        return files.toSortedMap().flatMap { (path, file) ->
            val entry = entries.firstOrNull { it.matches(path) }
            val lines = file.readLines()
            if (entry == null) {
                listOf("$path: no entry of config/markdown/limits.yml lists this file. Add the text to a listed file.")
            } else {
                checkLineLength(path, lines) + checkEntry(path, lines, entry, files) +
                    checkAdr(path, lines, entry, files, adrs.toSet())
            }
        } + AdrStructure.checkNumbering(adrs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkAdr(
        path: String,
        lines: List<String>,
        entry: MarkdownEntry,
        files: Map<String, File>,
        adrs: Set<String>
    ): List<String> {
        val adr = entry.adr ?: return emptyList()
        val template = adr["template"] as String
        val structure =
            AdrStructure(
                template = files[template]?.readLines() ?: error("limits.yml names the missing template $template."),
                maxSectionLines = (adr["maxSectionLines"] as Map<String, Int>?).orEmpty(),
                minOptions = adr["minOptions"] as Int,
                maxOptions = adr["maxOptions"] as Int,
                source = entry.source
            )
        return structure.check(path, lines, adrs)
    }

    private fun checkLineLength(
        path: String,
        lines: List<String>
    ): List<String> =
        MarkdownLines(lines).prose().filter { (_, line) -> line.length > lineLength }.map { (index, line) ->
            "$path:${index + 1}: the line has ${line.length} characters, more than $lineLength. Wrap it."
        }

    private fun checkEntry(
        path: String,
        lines: List<String>,
        entry: MarkdownEntry,
        files: Map<String, File>
    ): List<String> {
        val violations = mutableListOf<String>()
        val unscoped =
            if (entry.includeUnscopedRules) {
                files.filterKeys {
                    it.startsWith(".claude/rules/") &&
                        !MarkdownLines(files.getValue(it).readLines()).scoped()
                }
            } else {
                emptyMap()
            }
        val total = lines.size + unscoped.values.sumOf { it.readLines().size }
        entry.maxLines?.takeIf { total > it }?.let { max ->
            val counted = if (unscoped.isEmpty()) "" else " with ${unscoped.keys.joinToString()}"
            violations += "$path: $total lines$counted, more than $max (${entry.source}). Remove text that a check " +
                "already enforces or that describes the code, or move text to a file that a path-scoped rule loads."
        }
        entry.maxSectionLines?.let { max ->
            MarkdownLines(lines).sections().filter { it.second > max }.forEach { (heading, size) ->
                violations += "$path:${heading + 1}: the section has $size lines, more than $max (${entry.source})."
            }
        }
        if (entry.changelog) {
            violations +=
                ChangelogStructure(entry.maxBulletLines ?: 2, entry.source).check(path, lines)
        }
        return violations
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun parse(text: String): MarkdownLimits {
            val root = Yaml().load<Map<String, Any?>>(text)
            val entries =
                (root["files"] as List<Map<String, Any?>>).map {
                    MarkdownEntry(
                        path = it["path"] as String?,
                        glob = it["glob"] as String?,
                        maxLines = it["maxLines"] as Int?,
                        maxSectionLines = it["maxSectionLines"] as Int?,
                        includeUnscopedRules = it["includeUnscopedRules"] as Boolean? ?: false,
                        changelog = it["changelog"] as Boolean? ?: false,
                        maxBulletLines = it["maxBulletLines"] as Int?,
                        adr = it["adr"] as Map<String, Any?>?,
                        source = it["source"] as String? ?: error("An entry of limits.yml has no source.")
                    )
                }
            return MarkdownLimits(root["lineLength"] as Int, entries)
        }
    }
}
