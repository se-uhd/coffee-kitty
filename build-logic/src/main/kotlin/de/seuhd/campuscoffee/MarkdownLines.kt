package de.seuhd.campuscoffee

/** The lines of a Markdown file, split into front matter, code blocks, tables, and prose. */
class MarkdownLines(
    private val lines: List<String>
) {
    private val frontMatterEnd =
        if (lines.firstOrNull() == "---") lines.drop(1).indexOf("---").let { if (it < 0) 0 else it + 2 } else 0

    /** Returns whether the front matter names `paths`, so that Claude Code loads the file only for these paths. */
    fun scoped(): Boolean = lines.take(frontMatterEnd).any { it.startsWith("paths:") }

    /** Returns the index and the text of each line outside the front matter, code blocks, tables, and link targets. */
    fun prose(): List<Pair<Int, String>> {
        var inCode = false
        return lines.withIndex().mapNotNull { (index, line) ->
            val fence = line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")
            if (fence) inCode = !inCode
            val skipped =
                index < frontMatterEnd || fence || inCode || line.trimStart().startsWith("|") ||
                    LINK_TARGET.matches(line)
            if (skipped) null else index to line
        }
    }

    /** Returns the index of each heading with the number of lines up to the next heading. */
    fun sections(): List<Pair<Int, Int>> {
        val headings = prose().filter { (_, line) -> line.startsWith("#") }.map { it.first }
        val ends = headings.drop(1) + lines.size
        return headings.zip(ends) { heading, end -> heading to end - heading - 1 }
    }

    private companion object {
        val LINK_TARGET = Regex("""^\[[^]]+]: \S+$""")
    }
}
