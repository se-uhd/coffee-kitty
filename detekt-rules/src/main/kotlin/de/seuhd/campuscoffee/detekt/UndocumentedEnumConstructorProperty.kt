package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass

/**
 * Reports each non-private constructor property of an enum class without an `@property` or `@param` tag. A constructor
 * property is a `val` or `var` parameter of the primary constructor. The rule checks only enum classes with a KDoc,
 * because [UndocumentedNonLocalClass] reports a missing KDoc.
 */
class UndocumentedEnumConstructorProperty(
    config: Config
) : Rule(config, "Non-private enum constructor properties require @property or @param.") {
    override fun visitClass(klass: KtClass) {
        checkEnumProperties(klass)
        super.visitClass(klass)
    }

    /**
     * Reports each non-private constructor property of [klass] without an `@property` or `@param` tag. Reports nothing
     * when [klass] is not an enum class or has no KDoc.
     */
    private fun checkEnumProperties(klass: KtClass) {
        if (!klass.isEnum()) return
        val documented = klass.docComment?.tagSubjects(KDocKnownTag.PARAM, KDocKnownTag.PROPERTY) ?: return
        klass.primaryConstructor
            ?.valueParameters
            .orEmpty()
            .filter { it.hasValOrVar() && !it.hasModifier(KtTokens.PRIVATE_KEYWORD) }
            .filter { it.name != null && it.name !in documented }
            .forEach { parameter ->
                report(
                    Finding(
                        Entity.atName(parameter),
                        "Enum property '${parameter.nameAsSafeName}' is missing an @property or @param."
                    )
                )
            }
    }
}
