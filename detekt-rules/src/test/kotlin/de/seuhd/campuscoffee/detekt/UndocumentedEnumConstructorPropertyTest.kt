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
}
