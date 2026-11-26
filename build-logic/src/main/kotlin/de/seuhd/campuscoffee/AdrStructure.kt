package de.seuhd.campuscoffee

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The structure of an ADR after `doc/adr/template.md` (ADR 016): the sections and metadata of the template in its
 * order, a status from the status line of the template, a date, the people involved, a number of options, and section
 * lengths.
 *
 * @property maxSectionLines the most lines with text in a section, by the name of its heading, such as `Context`
 */
class AdrStructure(
    template: List<String>,
    private val maxSectionLines: Map<String, Int>,
    private val minOptions: Int,
    private val maxOptions: Int,
    private val source: String
) {
    private val sections = withoutComments(template).filter { it.startsWith("## ") }
    private val metadata = headingsBelow(withoutComments(template), "## Decision Metadata")
    private val statuses =
        firstLine(withoutComments(template), "### Status")?.split("/")
            ?: error("doc/adr/template.md has no status line.")

    fun check(
        path: String,
        lines: List<String>,
        allAdrPaths: Set<String>
    ): List<String> {
        val violations = mutableListOf<String>()
        val prose = withoutComments(MarkdownLines(lines).prose().map { it.second })
        if (!FILE.matches(path)) violations += "$path: the name of an ADR is NNN-name-in-kebab-case.md."
        if (prose.firstOrNull()?.startsWith("# ") != true || prose.count { it.startsWith("# ") } != 1) {
            violations += "$path: an ADR starts with its title as the only heading '# '."
        }
        if (prose.filter { it.startsWith("## ") } != sections) {
            violations +=
                "$path: the sections are ${sections.joinToString()}, in this order, as in doc/adr/template.md."
        }
        if (headingsBelow(prose, "## Decision Metadata") != metadata) {
            violations += "$path: the metadata has the headings ${metadata.joinToString()}, in this order."
        }
        val status = firstParagraph(prose, "### Status")
        if (status == null || !isValidStatus(status, allAdrPaths)) {
            violations += "$path: the status is one of ${statuses.joinToString("/")} from doc/adr/template.md, and " +
                "$SUPERSEDED_STATUS takes ' by [ADR-NNN](NNN-name.md)'. Another status takes ', amended by " +
                "[ADR-NNN](NNN-name.md)' for each later ADR that changes a part of it (ADR 017). Each link names an ADR."
        }
        if (firstLine(prose, "### Date")?.let(::isDate) != true) violations += "$path: the date is YYYY-MM-DD."
        if (body(prose, "### Involved").none { it.startsWith("* ") }) violations += "$path: Involved lists a person."
        val options = body(prose, "## Considered Options").count { it.startsWith("* ") }
        if (options !in minOptions..maxOptions) {
            violations += "$path: the ADR considers $options options, not $minOptions to $maxOptions ($source)."
        }
        maxSectionLines.forEach { (name, max) ->
            val size = body(prose, "## $name").count { it.isNotBlank() }
            if (size > max) violations += "$path: the section $name has $size lines, more than $max ($source)."
        }
        return violations
    }

    private fun isValidStatus(
        status: String,
        allAdrPaths: Set<String>
    ): Boolean {
        val amendable = statuses.filter { it != SUPERSEDED_STATUS }.joinToString("|") { Regex.escape(it) }
        val form =
            Regex("($amendable)(, amended by $LINK_PATTERN)*").matches(status) ||
                Regex("$SUPERSEDED_STATUS by $LINK_PATTERN").matches(status)
        return form && Regex(LINK_PATTERN).findAll(status).all { "doc/adr/${it.groupValues[1]}" in allAdrPaths }
    }

    private fun firstLine(
        prose: List<String>,
        heading: String
    ): String? = body(prose, heading).firstOrNull { it.isNotBlank() }

    private fun firstParagraph(
        prose: List<String>,
        heading: String
    ): String? =
        body(prose, heading)
            .dropWhile { it.isBlank() }
            .takeWhile { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { null }

    private fun body(
        prose: List<String>,
        heading: String
    ): List<String> {
        val start = prose.indexOf(heading)
        if (start < 0) return emptyList()
        val level = heading.substringBefore(" ")
        return prose.drop(start + 1).takeWhile { line ->
            !line.startsWith("#") || line.substringBefore(" ").length > level.length
        }
    }

    private fun headingsBelow(
        lines: List<String>,
        heading: String
    ): List<String> =
        lines.dropWhile { it != heading }.drop(1).takeWhile { !it.startsWith("## ") }.filter {
            it.startsWith("### ")
        }

    private fun withoutComments(lines: List<String>): List<String> {
        var inComment = false
        return lines.filter { line ->
            if (line.trimStart().startsWith("<!--")) inComment = true
            val kept = !inComment
            if (inComment && line.contains("-->")) inComment = false
            kept
        }
    }

    private fun isDate(text: String): Boolean =
        try {
            LocalDate.parse(text)
            true
        } catch (_: DateTimeParseException) {
            false
        }

    companion object {
        private val FILE = Regex("""doc/adr/\d{3}-[a-z0-9]+(-[a-z0-9]+)*\.md""")
        private const val SUPERSEDED_STATUS = "Superseded"
        private const val LINK_PATTERN = """\[ADR-\d{3}]\((\d{3}-[a-z0-9-]+\.md)\)"""

        fun checkNumbering(adrPaths: Collection<String>): List<String> {
            val numbers =
                adrPaths.mapNotNull {
                    Regex("""doc/adr/(\d{3})-""")
                        .find(it)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()
                }
            val last = numbers.maxOrNull() ?: 0
            return if (numbers.sorted() == (1..last).toList()) {
                emptyList()
            } else {
                listOf("doc/adr: the ADRs are numbered 001 to ${"%03d".format(last)} without a gap or a duplicate.")
            }
        }
    }
}
