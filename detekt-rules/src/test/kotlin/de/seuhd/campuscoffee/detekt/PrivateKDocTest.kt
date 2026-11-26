package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrivateKDocTest {
    private val rule = PrivateKDoc(Config.empty)

    @Test
    fun `PrivateKDoc reports a KDoc on a private class, object, function, property, and type alias`() {
        val code =
            "/** A class. */\nprivate class Hidden(val a: Int)\n\n/** An object. */\nprivate object Single\n\n" +
                "/** A function. */\nprivate fun f() = 1\n\n/** A property. */\nprivate val p = 1\n\n" +
                "/** An alias. */\nprivate typealias Names = List<String>"

        assertEquals(listOf("Hidden", "Single", "f", "p", "Names"), rule.reportedNames(code))
    }

    @Test
    fun `PrivateKDoc reports a KDoc on a private member and a private constructor property`() {
        val code =
            "class Visible(\n    /** A parameter. */\n    private val a: Int\n) {\n    /** A function. */\n" +
                "    private fun f() = a\n\n    /** A nested class. */\n    private class Nested\n}"

        assertEquals(listOf("a", "f", "Nested"), rule.reportedNames(code))
    }

    @Test
    fun `PrivateKDoc accepts a KDoc on a declaration that is not private and a private declaration without KDoc`() {
        val code =
            "/** A class. */\nclass Visible {\n    /** A function. */\n    internal fun f() = 1\n\n" +
                "    private fun g() = 2\n}\n\nprivate class Hidden {\n    /** A member. */\n    fun h() = 3\n}"

        assertEquals(emptyList<String>(), rule.reportedNames(code))
    }
}
