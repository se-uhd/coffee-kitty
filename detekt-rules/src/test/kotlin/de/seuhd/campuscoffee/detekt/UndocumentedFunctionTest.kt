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

    @Test
    fun `UndocumentedFunction reports no function with KDoc, no override, and no local function`() {
        val code =
            """
            /** A shape. */
            interface Shape {
                /** Returns the area. */
                fun area(): Double
            }

            /** A square. */
            class Square : Shape {
                override fun area(): Double {
                    fun side() = 2.0
                    return side() * side()
                }
            }
            """.trimIndent()
        assertEquals(emptyList<String?>(), rule.reportedNames(code))
    }
}
