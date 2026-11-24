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
}
