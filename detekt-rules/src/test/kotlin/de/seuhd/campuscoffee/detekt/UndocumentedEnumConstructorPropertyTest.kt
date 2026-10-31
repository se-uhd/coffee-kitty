package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UndocumentedEnumConstructorPropertyTest {
    private val rule = UndocumentedEnumConstructorProperty(Config.empty)

    @Test
    fun `UndocumentedEnumConstructorProperty reports a non-private enum property without @property`() {
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
    fun `UndocumentedEnumConstructorProperty reports nothing for a documented enum property`() {
        val code =
            """
            /**
             * Sizes.
             * @property label the short label
             */
            enum class Size(val label: String) {
                SMALL("S"),
                LARGE("L")
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedEnumConstructorProperty reports nothing for a private enum property`() {
        val code =
            """
            /** Sizes. */
            enum class Size(private val label: String) {
                SMALL("S"),
                LARGE("L")
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }

    @Test
    fun `UndocumentedEnumConstructorProperty reports nothing for an enum property documented with @param`() {
        val code =
            """
            /**
             * Sizes.
             * @param label the short label
             */
            enum class Size(val label: String) {
                SMALL("S"),
                LARGE("L")
            }
            """.trimIndent()
        assertTrue(rule.lintSource(code).isEmpty())
    }
}
