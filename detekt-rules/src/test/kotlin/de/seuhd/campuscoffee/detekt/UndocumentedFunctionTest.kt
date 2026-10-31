package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UndocumentedFunctionTest {
    private val rule = UndocumentedFunction(Config.empty)

    @Test
    fun `UndocumentedFunction reports a non-local function without KDoc`() {
        assertEquals(1, rule.lintSource("fun answer() = 42").size)
    }

    @Test
    fun `UndocumentedFunction reports nothing for a documented function`() {
        val code =
            """
            /** Returns the answer. */
            fun answer() = 42
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedFunction reports nothing for an override function`() {
        val code =
            """
            /** A base. */
            interface Base {
                /** Runs it. */
                fun run()
            }

            /** An implementation. */
            class Impl : Base {
                override fun run() = Unit
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedFunction reports nothing for a local function`() {
        val code =
            """
            /** Outer. */
            fun outer() {
                fun local() = 1
                local()
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
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
