package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * Reports a KDoc on a private declaration, such as a class, an object, a function, or a property, because only a
 * contract has KDoc (doc/adr/013-limits-for-text.md). detekt's `DocumentationOverPrivateFunction` and
 * `DocumentationOverPrivateProperty` leave out the private classes and objects.
 */
class PrivateKDoc(
    config: Config
) : Rule(config, "A private declaration has no KDoc.") {
    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
        super.visitNamedDeclaration(declaration)
        if (declaration.docComment != null && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            report(Finding(Entity.atName(declaration), "The private ${declaration.nameAsSafeName} has KDoc. $ADVICE"))
        }
    }

    private companion object {
        const val ADVICE = "Delete the KDoc, and move a reason into an ADR with '// ADR-NNN <text>'."
    }
}
