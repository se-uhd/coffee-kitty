package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.test.utils.compileContentForTest
import dev.detekt.test.utils.compileForTest
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

/**
 * Compiles the Kotlin snippet [code] and runs this rule on the compiled file.
 *
 * @param code the Kotlin source to check
 * @return the findings that this rule reports for [code]
 */
internal fun Rule.lintSource(code: String): List<Finding> =
    visitFile(compileContentForTest(code), LanguageVersionSettingsImpl.DEFAULT)

/**
 * Runs this rule on the Kotlin snippet [code] and returns the name of each declaration that the rule reports.
 *
 * @param code the Kotlin source to check
 */
internal fun Rule.reportedNames(code: String): List<String?> =
    lintSource(code).map { (it.entity.ktElement as KtNamedDeclaration).name }

/**
 * Writes the Kotlin snippet [code] into `src/Test.kt` of [repository], whose `doc/adr/` holds an ADR for each of [adrs],
 * and returns the messages of the findings that this rule reports for the file.
 *
 * @param repository the directory that stands for the repository
 * @param code the Kotlin source to check
 * @param adrs the three-digit numbers of the ADRs that exist
 */
internal fun Rule.lintSourceIn(
    repository: Path,
    code: String,
    vararg adrs: String
): List<String> {
    val decisions = repository.resolve("doc/adr").createDirectories()
    adrs.forEach { decisions.resolve("$it-a-decision.md").createFile() }
    val source = repository.resolve("src").createDirectories().resolve("Test.kt")
    source.writeText(code)
    return visitFile(compileForTest(source), LanguageVersionSettingsImpl.DEFAULT).map { it.message }
}
