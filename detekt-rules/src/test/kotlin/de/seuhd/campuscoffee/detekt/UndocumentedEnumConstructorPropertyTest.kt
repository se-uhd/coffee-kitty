package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UndocumentedEnumConstructorPropertyTest {
    private val rule = UndocumentedEnumConstructorProperty(Config.empty)

    @Test
    fun `UndocumentedEnumConstructorProperty reports a non-private enum property without @property or @param`() {
        val code =
            """
            /** Sizes. */
            enum class Size(val label: String) {
                SMALL("S"),
                LARGE("L")
            }
            """.trimIndent()
        assertEquals(1, rule.lintSource(code).size)
    }

    @Test
    fun `UndocumentedEnumConstructorProperty reports no property with @property or @param and no private property`() {
        val code =
            """
            /**
             * Sizes.
             *
             * @property label the label
             * @param code the code
             */
            enum class Size(val label: String, val code: Int, private val hidden: Int, plain: Int) {
                SMALL("S", 1, 0, 0)
            }
            """.trimIndent()
        assertEquals(emptyList<String?>(), rule.reportedNames(code))
    }

    @Test
    fun `UndocumentedEnumConstructorProperty reports no property of a class that is not an enum or has no KDoc`() {
        val code =
            """
            /** A point. */
            class Point(val x: Int)

            enum class Undocumented(val label: String) {
                ONE("1")
            }
            """.trimIndent()
        assertEquals(emptyList<String?>(), rule.reportedNames(code))
    }
}
