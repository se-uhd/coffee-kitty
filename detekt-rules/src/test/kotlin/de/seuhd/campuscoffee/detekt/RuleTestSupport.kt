package de.seuhd.campuscoffee.detekt

import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.test.utils.compileContentForTest
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

/**
 * Compiles the Kotlin snippet [code] and runs this rule on the compiled file.
 *
 * @param code the Kotlin source to check
 * @return the findings that this rule reports for [code]
 */
internal fun Rule.lintSource(code: String): List<Finding> =
    visitFile(compileContentForTest(code), LanguageVersionSettingsImpl.DEFAULT)
