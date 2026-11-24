package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(listOf("Hidden"), rule.reportedNames(code))
    }

    @Test
    fun `UndocumentedNonLocalClass reports an enum class without KDoc but not its entries`() {
        assertEquals(listOf("Color"), rule.reportedNames("enum class Color { RED, GREEN }"))
    }

    @Test
    fun `UndocumentedNonLocalClass reports an interface and an object without KDoc, one finding each`() {
        val code =
            """
            interface Shape
            object Registry
            """.trimIndent()
        assertEquals(listOf("Shape", "Registry"), rule.reportedNames(code))
    }

    @Test
    fun `UndocumentedNonLocalClass reports no class with KDoc, companion object, local class, or anonymous object`() {
        val code =
            """
            /** A holder. */
            class Holder {
                companion object {
                    const val SIZE = 1
                }

                /** Runs. */
                fun run(): Any {
                    class Local
                    return object : Any() {}
                }
            }
            """.trimIndent()
        assertEquals(emptyList<String?>(), rule.reportedNames(code))
    }
}
