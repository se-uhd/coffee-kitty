package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KDocLengthTest {
    private val rule = KDocLength(Config.empty)

    private fun kdoc(lines: Int) = "/**\n" + (1..lines).joinToString("") { " * Line $it.\n" }

    @Test
    fun `KDocLength accepts 6 lines on a type and 3 on a function, with one line per tag`() {
        val code =
            kdoc(6) + " */\nclass Holder {\n" + kdoc(3) + " *\n * @param x the value\n */\n    fun f(x: Int) = x\n}"

        assertEquals(0, rule.lintSource(code).size)
    }

    @Test
    fun `KDocLength reports 7 lines on a type and 4 on a function`() {
        val code = kdoc(7) + " */\nclass Holder {\n" + kdoc(4) + " */\n    fun f() = 1\n}"

        assertEquals(2, rule.lintSource(code).size)
    }

    @Test
    fun `KDocLength reports a tag that takes two lines`() {
        val code = "/**\n * Adds.\n *\n * @param x the value,\n *   which is positive\n */\nfun f(x: Int) = x"

        assertEquals(1, rule.lintSource(code).size)
    }
}
