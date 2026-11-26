package de.seuhd.campuscoffee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TextChecksTest {
    private val changelog = ChangelogStructure(maxBulletLines = 2, source = "test")

    private fun changelog(vararg body: String) =
        changelog.check("CHANGELOG.md", listOf("# Changelog", "") + body.flatMap { it.split("\n") })

    @Test
    fun `ChangelogStructure reports a bullet of three lines and a continuation after a blank line`() {
        val violations =
            changelog("## [Unreleased]", "", "### Added", "", "- one\n  two\n  three", "", "- four", "", "  five")

        assertEquals(
            listOf(
                "CHANGELOG.md:7: the bullet has more than 2 lines (test).",
                "CHANGELOG.md:13: a section holds only bullets ('- ') and their continuation lines (two spaces)."
            ),
            violations
        )
    }

    @Test
    fun `ChangelogStructure reports sections out of the order of Keep a Changelog and a section without a bullet`() {
        val violations = changelog("## [Unreleased]", "", "### Fixed", "", "- a", "", "### Added", "", "### Security")

        assertEquals(listOf(9, 11, 12), violations.map { it.split(":")[1].toInt() }, violations.toString())
        assertTrue(violations[0].contains("in this order"))
        assertTrue(violations.drop(1).all { it.endsWith("has no bullet.") })
    }

    @Test
    fun `ChangelogStructure reports a version that is not older than the version above it`() {
        val violations = changelog("## [1.0.0] - 2026-01-01", "", "## [1.1.0] - 2026-02-01")

        assertEquals(listOf("CHANGELOG.md:5: the version 1.1.0 is not older than the version above it."), violations)
    }

    private val template =
        """
        # Title

        ## Decision Metadata

        ### Status

        Proposed/Rejected/Accepted/Deprecated/Superseded

        ### Date

        ### Involved

        ## Context

        ## Considered Options

        ## Decision(s)

        ## Consequences
        """.trimIndent().lines()

    private fun adr(
        status: String,
        options: Int
    ): List<String> =
        listOf("# Which option?", "", "## Decision Metadata", "", "### Status", "") + status.lines() +
            listOf("", "### Date", "", "2026-11-26", "", "### Involved", "", "* A Person", "", "## Context", "") +
            listOf("Text.", "", "## Considered Options", "") + (1..options).map { "* Option $it" } +
            listOf("", "## Decision(s)", "", "Text.", "", "## Consequences", "", "Text.")

    private val structure = AdrStructure(template, mapOf("Context" to 1), 2, 5, "test")
    private val adrs = setOf("doc/adr/001-one.md", "doc/adr/002-two.md")

    @Test
    fun `AdrStructure accepts a status of the template, an amended status on two lines, and a superseded status`() {
        val amended = adr("Accepted, amended by [ADR-001](001-one.md), amended by\n[ADR-002](002-two.md)", 2)
        val superseded = adr("Superseded by [ADR-002](002-two.md)", 5)

        assertEquals(emptyList<String>(), structure.check("doc/adr/002-two.md", adr("Accepted", 2), adrs))
        assertEquals(emptyList<String>(), structure.check("doc/adr/002-two.md", amended, adrs))
        assertEquals(emptyList<String>(), structure.check("doc/adr/001-one.md", superseded, adrs))
    }

    @Test
    fun `AdrStructure reports an unknown status, a link to a missing ADR, and too few or too many options`() {
        val missing = adr("Accepted, amended by [ADR-003](003-three.md)", 2)

        assertEquals(1, structure.check("doc/adr/002-two.md", adr("Approved", 2), adrs).size)
        assertEquals(1, structure.check("doc/adr/002-two.md", missing, adrs).size)
        assertTrue(structure.check("doc/adr/002-two.md", adr("Accepted", 1), adrs).single().contains("1 options"))
        assertTrue(structure.check("doc/adr/002-two.md", adr("Accepted", 6), adrs).single().contains("6 options"))
    }

    @Test
    fun `AdrStructure reports a gap in the numbers of the ADRs`() {
        assertEquals(emptyList<String>(), AdrStructure.checkNumbering(adrs))
        assertEquals(1, AdrStructure.checkNumbering(setOf("doc/adr/001-one.md", "doc/adr/003-three.md")).size)
    }

    @Test
    fun `MarkdownLimits reports an unlisted file and a long line outside a code block`(
        @TempDir root: File
    ) {
        val limits =
            MarkdownLimits.parse(
                """
                lineLength: 20
                files:
                  - path: listed.md
                    maxLines: 6
                    source: "test"
                """.trimIndent()
            )
        val listed = File(root, "listed.md").apply { writeText("short\n```\na code line that is long\n```\n") }
        val unlisted = File(root, "unlisted.md").apply { writeText("a\n") }

        assertEquals(emptyList<String>(), limits.check(mapOf("listed.md" to listed)))
        listed.writeText("a line that is too long\n")
        val violations = limits.check(mapOf("listed.md" to listed, "unlisted.md" to unlisted))
        assertEquals(2, violations.size, violations.toString())
    }

    @Test
    fun `RuleIndex lists each rule with its description, and AGENTS md as a table with the paths`() {
        val rule = listOf("---", "description: \"the tests.\"", "paths:", "  - \"src/test/**\"", "---", "", "# Tests")
        val index = RuleIndex(mapOf("tests.md" to rule), 120)
        val text = "intro\n${RuleIndex.BEGIN}\nold\n${RuleIndex.END}\n"

        assertEquals(
            "intro\n${RuleIndex.BEGIN}\n- `tests.md`: the tests.\n${RuleIndex.END}\n",
            index.render("CLAUDE.md", text)
        )
        assertTrue(index.render("AGENTS.md", text).contains("| `.claude/rules/tests.md` | `src/test/**` |"))
    }
}
