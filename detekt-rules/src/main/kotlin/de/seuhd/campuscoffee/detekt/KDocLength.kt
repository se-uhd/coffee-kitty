package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Reports a KDoc with more than `maxTypeLines` lines of description on a type or more than `maxMemberLines` on another
 * declaration, and a block tag such as `@param` that takes more than one line (doc/adr/013-limits-for-text.md).
 */
class KDocLength(
    config: Config
) : Rule(config, "A KDoc has at most 6 lines on a type and 3 on another declaration, and each tag has one line.") {
    private val maxTypeLines: Int by config(MAX_TYPE_LINES)
    private val maxMemberLines: Int by config(MAX_MEMBER_LINES)

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)
        val kdoc = dcl.docComment ?: return
        val lines =
            kdoc.text
                .removePrefix("/**")
                .removeSuffix("*/")
                .lines()
                .map { it.trim().removePrefix("*").trim() }
                .filter { it.isNotEmpty() }
        val description = lines.indexOfFirst { it.startsWith("@") }.let { if (it < 0) lines.size else it }
        val max = if (dcl is KtClassOrObject) maxTypeLines else maxMemberLines
        if (description > max) {
            report(Finding(Entity.from(dcl), "The KDoc has $description lines of description, more than $max. $ADVICE"))
        }
        val wrapped = lines.drop(description).count { !it.startsWith("@") }
        if (wrapped > 0) report(Finding(Entity.from(dcl), "A tag of the KDoc takes more than one line. $ADVICE"))
    }

    private companion object {
        const val MAX_TYPE_LINES = 6
        const val MAX_MEMBER_LINES = 3
        const val ADVICE =
            "Delete what the code or the name already says, and move a decision into an ADR."
    }
}
