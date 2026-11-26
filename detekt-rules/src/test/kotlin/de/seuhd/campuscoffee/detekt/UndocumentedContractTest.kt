package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UndocumentedContractTest {
    private val rule = UndocumentedContract(Config.empty)

    @Test
    fun `UndocumentedContract reports a port and its functions without KDoc`() {
        val code = "package app.ports.api\n\ninterface UserService {\n    fun getAll(): List<String>\n}"

        assertEquals(listOf("UserService", "getAll"), rule.reportedNames(code))
    }

    @Test
    fun `UndocumentedContract reports a Properties class without KDoc`() {
        assertEquals(
            listOf("GracePeriodProperties"),
            rule.reportedNames("class GracePeriodProperties(val minutes: Int)")
        )
    }

    @Test
    fun `UndocumentedContract reports no class outside the ports and no documented port`() {
        val code =
            "package app.ports.api\n\n/** The users. */\ninterface UserService {\n    /** Returns all. */\n" +
                "    fun getAll(): List<String>\n}\n\nclass Helper {\n    fun help() = 1\n}"

        assertEquals(emptyList<String>(), rule.reportedNames(code))
    }
}
