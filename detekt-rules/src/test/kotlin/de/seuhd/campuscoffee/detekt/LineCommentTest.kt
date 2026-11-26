package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineCommentTest {
    private val rule = LineComment(Config.empty)

    @Test
    fun `LineComment accepts a pointer to an ADR and an external constraint of up to three lines`() {
        val code =
            "// ADR-010\nval limit = 70\n\n// External: https://example.org/issue The library fails\n// for an empty " +
                "list, which the caller\n// cannot rule out.\nval list = listOf(1)"

        assertEquals(0, rule.lintSource(code).size)
    }

    @Test
    fun `LineComment reports other line comments and block comments`() {
        val code = "// Sets the limit.\nval limit = 70\n\n/* A block. */\nval other = 1\n\nval x = 1 // why"

        assertEquals(3, rule.lintSource(code).size)
    }

    @Test
    fun `LineComment reports an external constraint of four lines and an ADR pointer with a second line`() {
        val code =
            "// External: https://example.org a\n// b\n// c\n// d\nval x = 1\n\n// ADR-010\n// The limit.\nval y = 2"

        assertEquals(2, rule.lintSource(code).size)
    }
}
