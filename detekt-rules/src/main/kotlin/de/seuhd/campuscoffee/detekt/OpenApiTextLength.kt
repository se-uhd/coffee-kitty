package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Reports a text of a Swagger annotation, such as `@Operation` or `@field:Schema`, that is too long: a `summary` or a
 * `title` with more than `maxSummaryLength` characters, a `summary` with more than one sentence, a `description` of
 * `@Operation` or `@Info` with more than `maxLongDescriptionLength`, and any other `description` with more than
 * `maxDescriptionLength`. A text is the joined string literals of the argument (doc/adr/013-limits-for-text.md).
 */
class OpenApiTextLength(
    config: Config
) : Rule(config, "An OpenAPI summary has one sentence of at most 60 characters, and a description at most 120.") {
    private val maxSummaryLength: Int by config(MAX_SUMMARY_LENGTH)
    private val maxDescriptionLength: Int by config(MAX_DESCRIPTION_LENGTH)
    private val maxLongDescriptionLength: Int by config(MAX_LONG_DESCRIPTION_LENGTH)

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        val name = annotationEntry.typeReference?.text ?: return
        val arguments = annotationEntry.valueArguments.filterIsInstance<KtValueArgument>()
        check(annotationEntry.containingKtFile, name, arguments)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeExpression?.text ?: return
        if (expression.getStrictParentOfType<KtAnnotationEntry>() != null) {
            check(expression.containingKtFile, name, expression.valueArguments)
        }
    }

    private fun check(
        file: KtFile,
        name: String,
        arguments: List<KtValueArgument>
    ) {
        val annotation = swaggerAnnotation(file, name) ?: return
        arguments.forEach { check(annotation, it) }
    }

    private fun check(
        annotation: String,
        argument: KtValueArgument
    ) {
        val attribute = argument.getArgumentName()?.asName?.asString()
        val max = attribute?.let { maxLength(annotation, it) }
        val text = argument.getArgumentExpression()?.let(::textOf)
        if (attribute == null || max == null || text == null) return
        val where = "The $attribute of @$annotation"
        if (text.length > max) {
            report(Finding(Entity.from(argument), "$where has ${text.length} characters, more than $max. $ADVICE"))
        }
        if (attribute == "summary" && SENTENCE_BREAK.containsMatchIn(text)) {
            report(Finding(Entity.from(argument), "$where has more than one sentence. $ADVICE"))
        }
    }

    private fun maxLength(
        annotation: String,
        attribute: String
    ): Int? =
        when {
            attribute == "summary" || attribute == "title" -> maxSummaryLength
            attribute != "description" -> null
            annotation in LONG_DESCRIPTIONS -> maxLongDescriptionLength
            else -> maxDescriptionLength
        }

    private fun swaggerAnnotation(
        file: KtFile,
        name: String
    ): String? {
        val fqName =
            if ('.' in name) {
                name
            } else {
                file.importDirectives
                    .firstOrNull { (it.aliasName ?: it.importedFqName?.shortName()?.asString()) == name }
                    ?.importedFqName
                    ?.asString()
            }
        return fqName?.takeIf { it.startsWith(SWAGGER_PACKAGE) }?.substringAfterLast('.')
    }

    private fun textOf(expression: KtExpression): String? =
        when (expression) {
            is KtStringTemplateExpression -> {
                expression.entries.joinToString("") {
                    if (it is KtEscapeStringTemplateEntry) it.unescapedValue else it.text
                }
            }

            is KtBinaryExpression -> {
                if (expression.operationToken == KtTokens.PLUS) {
                    val left = expression.left?.let(::textOf)
                    val right = expression.right?.let(::textOf)
                    if (left != null && right != null) left + right else null
                } else {
                    null
                }
            }

            is KtParenthesizedExpression -> {
                expression.expression?.let(::textOf)
            }

            else -> {
                null
            }
        }

    private companion object {
        const val MAX_SUMMARY_LENGTH = 60
        const val MAX_DESCRIPTION_LENGTH = 120
        const val MAX_LONG_DESCRIPTION_LENGTH = 360
        const val SWAGGER_PACKAGE = "io.swagger.v3.oas.annotations."
        val LONG_DESCRIPTIONS = setOf("Operation", "Info")
        val SENTENCE_BREAK = Regex("""[.!?]\s+[A-Z]""")
        const val ADVICE =
            "State one point, and leave the details to the schema, the responses, or the KDoc of the code."
    }
}
