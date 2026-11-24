package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UndocumentedFunctionTest {
    private val rule = UndocumentedFunction(Config.empty)

    @Test
    fun `UndocumentedFunction reports a non-local function without KDoc`() {
        assertEquals(1, rule.lintSource("fun answer() = 42").size)
    }

    @Test
    fun `UndocumentedFunction reports a private function without KDoc`() {
        val code =
            """
            /** A holder. */
            class Holder {
                private fun secret() = 1
            }
            """.trimIndent()
        assertEquals(1, rule.lintSource(code).size)
    }
}
