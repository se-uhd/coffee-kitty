package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UndocumentedNonLocalClassTest {
    private val rule = UndocumentedNonLocalClass(Config.empty)

    @Test
    fun `UndocumentedNonLocalClass reports a class without KDoc`() {
        assertEquals(1, rule.lintSource("class Widget").size)
    }

    @Test
    fun `UndocumentedNonLocalClass reports a private nested class without KDoc`() {
        val code =
            """
            /** A holder. */
            class Holder {
                private class Hidden
            }
            """.trimIndent()
        assertEquals(listOf("The Hidden type is missing KDoc."), rule.lintSource(code).map { it.message })
    }

    @Test
    fun `UndocumentedNonLocalClass reports nothing for a documented class`() {
        val code =
            """
            /** A widget. */
            class Widget
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedNonLocalClass reports an enum class without KDoc`() {
        assertEquals(1, rule.lintSource("enum class Color { RED, GREEN }").size)
    }

    @Test
    fun `UndocumentedNonLocalClass reports nothing for enum entries without KDoc`() {
        val code =
            """
            /** Colors. */
            enum class Color { RED, GREEN }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedNonLocalClass reports an interface and an object without KDoc, one finding each`() {
        val code =
            """
            interface Shape
            object Registry
            """.trimIndent()
        assertEquals(
            listOf("The Shape type is missing KDoc.", "The Registry type is missing KDoc."),
            rule.lintSource(code).map { it.message }
        )
    }

    @Test
    fun `UndocumentedNonLocalClass reports nothing for a companion object without KDoc`() {
        val code =
            """
            /** A holder. */
            class Holder {
                companion object {
                    /** Makes one. */
                    fun create() = Holder()
                }
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedNonLocalClass reports nothing for a local class`() {
        val code =
            """
            /** Outer. */
            fun outer() {
                class Local
                Local()
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }
}
