package de.seuhd.campuscoffee.tests.acceptance

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

/**
 * Runs the acceptance scenarios with Cucumber on the JUnit Platform. The path of the HTML report below is relative to
 * the working directory of the Gradle test task, which is the module directory. The report is therefore written to
 * `application/build/reports/cucumber/cucumber.html`, and `gradle clean` deletes it.
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
// Turns off the glue hint of Cucumber 8, which only logs a suggestion. The hint calls getDeclaringClass() on every
// scanned class. That call throws IncompatibleClassChangeError for a class that Kotlin generates for an inlined
// reified call, such as returnResult<T>().
@ConfigurationParameter(
    key = Constants.GLUE_HINT_ENABLED_PROPERTY_NAME,
    value = "false"
)
class CucumberTests
