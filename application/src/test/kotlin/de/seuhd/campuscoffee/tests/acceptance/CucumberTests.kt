package de.seuhd.campuscoffee.tests.acceptance

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

/**
 * Test runner for the Cucumber tests. The HTML report goes to `build/reports/cucumber/cucumber.html`, relative to
 * the module directory (Gradle's test working directory), so `gradle clean` removes it.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("de.seuhd.campuscoffee.tests.acceptance")
@ConfigurationParameter(
    key = Constants.PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber/cucumber.html"
)
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME,
    value = "de.seuhd.campuscoffee.tests.acceptance"
)
// Cucumber 8's glue hint calls getDeclaringClass() on every scanned class, which throws IncompatibleClassChangeError
// for the classes that Kotlin generates for an inlined reified call such as returnResult<T>(). The hint only logs a
// suggestion. The key is a string rather than the constant, which Cucumber 7 does not have.
@ConfigurationParameter(
    key = "cucumber.glue.hint.enabled",
    value = "false"
)
class CucumberTests
