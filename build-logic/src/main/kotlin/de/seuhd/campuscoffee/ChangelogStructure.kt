package de.seuhd.campuscoffee

/**
 * The structure of CHANGELOG.md after Keep a Changelog 1.1.0: a version heading per release, newest first, the
 * sections in the order of [SECTIONS], and only bullets of at most [maxBulletLines] lines in each section.
 */
class ChangelogStructure(
    private val maxBulletLines: Int,
    private val source: String
) {
    fun check(
        path: String,
        lines: List<String>
    ): List<String> {
        val scan = Scan(path)
        if (lines.firstOrNull() != "# Changelog") scan.fail(0, "the file starts with the heading '# Changelog'.")
        lines.withIndex().drop(1).forEach { (index, line) -> scan.read(index, line) }
        scan.endSection(lines.size)
        return scan.violations
    }

    private inner class Scan(
        private val path: String
    ) {
        val violations = mutableListOf<String>()
        private var versions = listOf<String>()
        private var section = -1
        private var bullets = 0
        private var bulletStart = -1
        private var bulletLines = 0

        fun fail(
            index: Int,
            message: String
        ) {
            violations += "$path:${index + 1}: $message"
        }

        fun endSection(index: Int) {
            if (section >= 0 && bullets == 0) fail(index, "the section ${SECTIONS[section]} has no bullet.")
        }

        fun read(
            index: Int,
            line: String
        ) {
            when {
                line.isBlank() -> bulletStart = -1
                line.startsWith("## ") -> readVersion(index, line)
                line.startsWith("### ") -> readSection(index, line)
                versions.isNotEmpty() && !LINK_TARGET.matches(line) -> readEntry(index, line)
            }
        }

        private fun readVersion(
            index: Int,
            line: String
        ) {
            endSection(index)
            val version = VERSION.matchEntire(line)
            val dated = version?.groupValues?.get(2)?.isNotEmpty() == true
            val unreleased = version?.groupValues?.get(1) == UNRELEASED
            if (version == null || dated == unreleased) {
                fail(index, "a version heading is '## [Unreleased]' or '## [x.y.z] - YYYY-MM-DD'.")
            }
            version?.groupValues?.get(1)?.let { name ->
                if (versions.isNotEmpty() && !newer(versions.last(), name)) {
                    fail(index, "the version $name is not older than the version above it.")
                }
                versions = versions + name
            }
            section = -1
            bullets = 0
        }

        private fun readSection(
            index: Int,
            line: String
        ) {
            endSection(index)
            val next = SECTIONS.indexOf(line.removePrefix("### "))
            if (versions.isEmpty() || next <= section) {
                fail(index, "the sections of a version are ${SECTIONS.joinToString()}, in this order, each once.")
            }
            section = maxOf(section, next)
            bullets = 0
        }

        private fun readEntry(
            index: Int,
            line: String
        ) {
            when {
                line.startsWith("- ") && section >= 0 -> {
                    bullets++
                    bulletStart = index
                    bulletLines = 1
                }

                line.startsWith("  ") && !line.startsWith("   ") && bulletStart >= 0 -> {
                    bulletLines++
                    if (bulletLines == maxBulletLines + 1) {
                        fail(bulletStart, "the bullet has more than $maxBulletLines lines ($source).")
                    }
                }

                else -> {
                    fail(index, "a section holds only bullets ('- ') and their continuation lines (two spaces).")
                }
            }
        }
    }

    private fun newer(
        above: String,
        below: String
    ): Boolean =
        when {
            above == UNRELEASED -> {
                below != UNRELEASED
            }

            below == UNRELEASED -> {
                false
            }

            else -> {
                val (a, b) = listOf(above, below).map { version -> version.split(".").map { it.toInt() } }
                a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x > y } ?: false
            }
        }

    private companion object {
        const val UNRELEASED = "Unreleased"
        val SECTIONS = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
        val VERSION = Regex("""^## \[(Unreleased|\d+\.\d+\.\d+)](?: - (\d{4}-\d{2}-\d{2}))?$""")
        val LINK_TARGET = Regex("""^\[[^]]+]: \S+$""")
    }
}
