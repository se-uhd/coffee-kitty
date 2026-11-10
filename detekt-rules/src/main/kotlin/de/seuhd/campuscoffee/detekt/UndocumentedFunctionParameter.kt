package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/**
 * Reports each value parameter of a public function that the KDoc of the function does not name in an `@param` tag. An
 * `@property` tag does not count. The rule skips local functions, overrides, and functions without a KDoc, because
 * [UndocumentedFunction] reports a missing KDoc.
 */
class UndocumentedFunctionParameter(
    config: Config
) : Rule(config, "Public functions must document every parameter with @param.") {
    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.isLocal || function.hasModifier(KtTokens.OVERRIDE_KEYWORD) || !function.isPublic) return
        val kdoc = function.docComment ?: return
        val documented = kdoc.tagSubjects(KDocKnownTag.PARAM)
        function.valueParameters
            .filter { it.name != null && it.name !in documented }
            .forEach { parameter ->
                report(
                    Finding(
                        Entity.atName(parameter),
                        "Parameter '${parameter.nameAsSafeName}' of ${function.nameAsSafeName} has no @param."
                    )
                )
            }
    }
}
