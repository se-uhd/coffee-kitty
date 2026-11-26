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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Reports each comment that is not a KDoc, unless it is `// ADR-NNN <text>` with the number of an ADR of `doc/adr/`,
 * which points to a decision, or `// External: <link> <reason>` of at most three lines, for a constraint of a library
 * or platform that the code cannot change. Comments on consecutive lines count as one comment
 * (doc/adr/013-limits-for-text.md, doc/adr/023-static-analysis-in-the-build.md).
 */
class LineComment(
    config: Config
) : Rule(config, "Comments other than KDoc are only '// ADR-NNN <text>' or '// External: <link> <reason>'.") {
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
        val adrs by lazy { adrNumbers(Path.of(file.virtualFilePath)) }
        for (group in groups) {
            val numbers = pointedAdrs(group)
            val message =
                when {
                    numbers != null -> numbers.firstOrNull { it !in adrs }?.let(::unknown)
                    external(group) -> null
                    else -> FORMAT
                }
            message?.let { report(Finding(Entity.from(group.first()), it)) }
        }
    }

    private fun firstText(group: List<PsiComment>): String =
        group
            .first()
            .text
            .removePrefix("//")
            .trim()

    private fun lineComments(group: List<PsiComment>): Boolean = group.all { it.tokenType == KtTokens.EOL_COMMENT }

    private fun external(group: List<PsiComment>): Boolean =
        lineComments(group) && group.size <= MAX_EXTERNAL_LINES && EXTERNAL.matches(firstText(group))

    private fun pointedAdrs(group: List<PsiComment>): List<String>? {
        val pointer = firstText(group)
        val text =
            POINTER
                .matchEntire(pointer)
                ?.groupValues
                ?.get(1)
                .orEmpty()
        val wellFormed = group.size == 1 && lineComments(group) && text.replace(ADR, "").any { it.isLetter() }
        return if (wellFormed) ADR.findAll(pointer).map { it.groupValues[1] }.toList() else null
    }

    private fun adrNumbers(source: Path): Set<String> {
        val directory =
            generateSequence(source.toAbsolutePath().parent) { it.parent }
                .map { it.resolve("doc/adr") }
                .firstOrNull { it.isDirectory() } ?: return emptySet()
        return Files.list(directory).use { paths ->
            paths
                .map { it.name }
                .filter { ADR_FILE.matches(it) }
                .map { it.take(ADR_DIGITS) }
                .toList()
                .toSet()
        }
    }

    private companion object {
        const val MAX_EXTERNAL_LINES = 3
        const val ADR_DIGITS = 3
        val ADR = Regex("""ADR-(\d{3})\b""")
        val POINTER = Regex("""ADR-\d{3} (.+)""")
        val ADR_FILE = Regex("""\d{3}-.+\.md""")
        val EXTERNAL = Regex("""External: https?://\S+ \S.*""")
        const val FORMAT =
            "Delete the comment. Encode a rule as a type, a test, or a check, and record a decision in an ADR with " +
                "'// ADR-NNN <text>'. For a constraint of a library or platform, write '// External: <link> <reason>'."

        fun unknown(number: String) =
            "The pointer names ADR-$number, which doc/adr/ does not hold. Point to the ADR that states the reason."
    }
}
