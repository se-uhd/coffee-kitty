package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LineCommentTest {
    private val rule = LineComment(Config.empty)

    @TempDir
    private lateinit var repository: Path

    private fun findings(code: String): List<String> = rule.lintSourceIn(repository, code, "010", "017")

    @Test
    fun `LineComment accepts a pointer with a text to an ADR and an external constraint of up to three lines`() {
        val code =
            "// ADR-010 the limit of the checks\nval limit = 70\n\n// ADR-010 the checks, ADR-017 the comments\n" +
                "val both = 2\n\n// External: https://example.org/issue The library fails\n// for an empty list, " +
                "which the caller\n// cannot rule out.\nval list = listOf(1)"

        assertEquals(emptyList<String>(), findings(code))
    }

    @Test
    fun `LineComment reports a pointer without a text and a pointer to an ADR that does not exist`() {
        val code =
            "// ADR-010\nval a = 1\n\n// ADR-010, ADR-017\nval b = 2\n\n// ADR-011 a text\nval c = 3\n\n" +
                "// ADR-010 a text, ADR-999 another\nval d = 4\n\n// ADR-0101 four digits\nval e = 5"

        assertEquals(listOf(FORMAT, FORMAT, UNKNOWN, UNKNOWN, FORMAT), findings(code).map { it.take(PREFIX) })
    }

    @Test
    fun `LineComment reports other line comments and block comments`() {
        val code = "// Sets the limit.\nval limit = 70\n\n/* A block. */\nval other = 1\n\nval x = 1 // why"

        assertEquals(3, findings(code).size)
    }

    @Test
    fun `LineComment reports an external constraint of four lines and an ADR pointer with a second line`() {
        val code =
            "// External: https://example.org a\n// b\n// c\n// d\nval x = 1\n\n// ADR-010 the limit\n// The limit." +
                "\nval y = 2"

        assertEquals(2, findings(code).size)
    }

    private companion object {
        const val PREFIX = 12
        val FORMAT = "Delete the comment.".take(PREFIX)
        val UNKNOWN = "The pointer names".take(PREFIX)
    }
}
