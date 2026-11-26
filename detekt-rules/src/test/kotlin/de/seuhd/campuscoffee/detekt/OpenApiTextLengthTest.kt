package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenApiTextLengthTest {
    private val rule = OpenApiTextLength(Config.empty)

    private fun text(length: Int) = "\"" + "a".repeat(length - 1) + ".\""

    private fun controller(
        summary: Int,
        schema: Int,
        body: Int,
        operation: Int,
        api: Int
    ) = """
        import io.swagger.v3.oas.annotations.OpenAPIDefinition
        import io.swagger.v3.oas.annotations.Operation
        import io.swagger.v3.oas.annotations.info.Info
        import io.swagger.v3.oas.annotations.media.Schema
        import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

        @OpenAPIDefinition(info = Info(title = "API", description = ${text(api)}))
        class Dto(@field:Schema(description = "a" + ${text(schema - 1)}) val x: Int)

        class Controller {
            @Operation(summary = ${text(summary)}, description = ${text(operation)})
            fun f(@ApiRequestBody(description = ${text(body)}) dto: Dto) = dto
        }
        """.trimIndent()

    @Test
    fun `OpenApiTextLength accepts a summary of 60 characters and descriptions of 120 and 360 characters`() {
        assertEquals(
            0,
            rule.lintSource(controller(summary = 60, schema = 120, body = 120, operation = 360, api = 360)).size
        )
    }

    @Test
    fun `OpenApiTextLength reports a summary of 61 characters and descriptions of 121 and 361 characters`() {
        assertEquals(
            5,
            rule.lintSource(controller(summary = 61, schema = 121, body = 121, operation = 361, api = 361)).size
        )
    }

    @Test
    fun `OpenApiTextLength reports a summary with two sentences`() {
        val code =
            "import io.swagger.v3.oas.annotations.Operation\n\n" +
                "@Operation(summary = \"Get a user. Admins only.\")\nfun f() = 1"

        assertEquals(1, rule.lintSource(code).size)
    }

    @Test
    fun `OpenApiTextLength reports no annotation outside the Swagger annotations`() {
        val code = "import app.Operation\n\n@Operation(summary = ${text(200)})\nfun f() = 1"

        assertEquals(0, rule.lintSource(code).size)
    }
}
