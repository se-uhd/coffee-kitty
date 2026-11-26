package de.seuhd.campuscoffee.detekt

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Reports each comment that is not a KDoc, unless it is `// ADR-NNN`, which points to a decision, or
 * `// External: <link> <reason>` of at most three lines, for a constraint of a library or platform that the code cannot
 * change. Comments on consecutive lines count as one comment (doc/adr/013-limits-for-text.md).
 */
class LineComment(
    config: Config
) : Rule(config, "Comments other than KDoc are only '// ADR-NNN' or '// External: <link> <reason>'.") {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val text = file.text
        val lineOf = { comment: PsiComment -> text.substring(0, comment.textRange.startOffset).count { it == '\n' } }
        val groups = mutableListOf<MutableList<PsiComment>>()
        for (comment in file.collectDescendantsOfType<PsiComment>().filter { it !is KDoc }) {
            val last = groups.lastOrNull()?.last()
            if (last != null && last.tokenType == KtTokens.EOL_COMMENT && lineOf(comment) == lineOf(last) + 1) {
                groups.last() += comment
            } else {
                groups += mutableListOf(comment)
            }
        }
        groups.filterNot(::allowed).forEach { report(Finding(Entity.from(it.first()), MESSAGE)) }
    }

    private fun allowed(group: List<PsiComment>): Boolean {
        if (group.any { it.tokenType != KtTokens.EOL_COMMENT }) return false
        val first =
            group
                .first()
                .text
                .removePrefix("//")
                .trim()
        return (ADR.matches(first) && group.size == 1) || (EXTERNAL.matches(first) && group.size <= MAX_EXTERNAL_LINES)
    }

    private companion object {
        const val MAX_EXTERNAL_LINES = 3
        val ADR = Regex("""ADR-\d{3}\b.*""")
        val EXTERNAL = Regex("""External: https?://\S+ \S.*""")
        const val MESSAGE =
            "Delete the comment. Encode a rule as a type, a test, or a check, and record a decision in an ADR with " +
                "'// ADR-NNN'. For a constraint of a library or platform, write '// External: <link> <reason>'."
    }
}
