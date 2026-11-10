package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * Reports every class, interface, object, and enum class without a KDoc. The rule skips enum entries, anonymous
 * objects, companion objects, and local types (types declared in a function body).
 */
class UndocumentedNonLocalClass(
    config: Config
) : Rule(config, "Non-local classes, interfaces, objects, and enums require KDoc.") {
    override fun visitClass(klass: KtClass) {
        reportIfUndocumented(klass)
        super.visitClass(klass)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        // A companion object is part of the enclosing class and needs no KDoc of its own.
        if (!declaration.isCompanion()) {
            reportIfUndocumented(declaration)
        }
        super.visitObjectDeclaration(declaration)
    }

    /**
     * Reports [element] when it has no KDoc. Skips enum entries, local types, and anonymous objects. An anonymous
     * object has no name identifier.
     */
    private fun reportIfUndocumented(element: KtClassOrObject) {
        if (element is KtEnumEntry || element.isLocal || element.nameIdentifier == null) return
        if (element.docComment != null) return
        report(
            Finding(
                Entity.atName(element),
                "The ${element.nameAsSafeName} type is missing KDoc."
            )
        )
    }
}
