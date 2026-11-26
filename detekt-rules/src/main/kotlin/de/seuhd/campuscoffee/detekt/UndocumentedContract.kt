package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass

/**
 * Reports a contract without KDoc: an interface in a package `ports`, each function that such an interface declares,
 * and a class whose name ends with `Properties`. Other code needs no KDoc (doc/adr/013-limits-for-text.md).
 */
class UndocumentedContract(
    config: Config
) : Rule(config, "Ports, their functions, and *Properties classes require KDoc.") {
    override fun visitClass(klass: KtClass) {
        val port =
            klass.isInterface() &&
                klass.containingKtFile.packageFqName
                    .pathSegments()
                    .any { it.asString() == "ports" }
        if ((port || klass.name?.endsWith("Properties") == true) && klass.docComment == null) {
            report(Finding(Entity.atName(klass), "The contract ${klass.nameAsSafeName} is missing KDoc."))
        }
        if (port) {
            klass.body
                ?.functions
                ?.filter { it.docComment == null && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
                ?.forEach {
                    report(
                        Finding(Entity.atName(it), "The port function ${it.nameAsSafeName} is missing KDoc.")
                    )
                }
        }
        super.visitClass(klass)
    }
}
