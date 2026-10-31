package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertEquals(listOf("Parameter 'a' of add has no @param."), rule.lintSource(code).map { it.message })
    }

    @Test
    fun `UndocumentedFunctionParameter reports nothing for a fully documented function`() {
        val code =
            """
            /**
             * Adds two numbers.
             * @param a the first addend
             * @param b the second addend
             */
            fun add(a: Int, b: Int) = a + b
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedFunctionParameter reports only the undocumented parameter of a partly documented function`() {
        val code =
            """
            /**
             * Adds two numbers.
             * @param a the first addend
             */
            fun add(a: Int, b: Int) = a + b
            """.trimIndent()
        val findings = rule.lintSource(code)
        assertEquals(listOf("Parameter 'b' of add has no @param."), findings.map { it.message })
    }

    @Test
    fun `UndocumentedFunctionParameter reports nothing for a function without KDoc`() {
        assertTrue(rule.lintSource("fun add(a: Int, b: Int) = a + b").isEmpty())
    }

    @Test
    fun `UndocumentedFunctionParameter reports nothing for a non-public function`() {
        val code =
            """
            /** A holder. */
            class Holder {
                /** Adds. */
                private fun add(a: Int, b: Int) = a + b
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }
}
