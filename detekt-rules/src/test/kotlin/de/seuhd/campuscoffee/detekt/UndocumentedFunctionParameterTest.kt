package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UndocumentedFunctionParameterTest {
    private val rule = UndocumentedFunctionParameter(Config.empty)

    @Test
    fun `UndocumentedFunctionParameter reports each parameter of a public function without @param`() {
        val code =
            """
            /** Adds two numbers. */
            fun add(a: Int, b: Int) = a + b
            """.trimIndent()
        assertEquals(2, rule.lintSource(code).size)
    }

    @Test
    fun `UndocumentedFunctionParameter reports a parameter that only an @property tag documents`() {
        val code =
            """
            /**
             * Adds.
             *
             * @property a the first number
             * @param b the second number
             */
            fun add(a: Int, b: Int) = a + b
            """.trimIndent()
        assertEquals(listOf("a"), rule.reportedNames(code))
    }

    @Test
    fun `UndocumentedFunctionParameter reports no parameter of a private, override, local, or undocumented function`() {
        val code =
            """
            /** A calculator. */
            interface Calculator {
                /**
                 * Adds.
                 *
                 * @param a the first number
                 * @param b the second number
                 */
                fun add(a: Int, b: Int): Int
            }

            /** A simple calculator. */
            class Simple : Calculator {
                override fun add(a: Int, b: Int): Int {
                    /** Doubles. */
                    fun twice(x: Int) = 2 * x
                    return twice(a) / 2 + negate(-b)
                }

                /** Negates. */
                private fun negate(x: Int) = -x

                fun undocumented(x: Int) = x
            }
            """.trimIndent()
        assertEquals(emptyList<String?>(), rule.reportedNames(code))
    }
}
