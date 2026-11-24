package de.seuhd.campuscoffee

import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

// PIT changes the code of a module, e.g., it negates a condition, and runs the tests of the module against each changed
// version (mutant). Only the domain module applies this plugin, because its unit tests run without Spring and
// PostgreSQL. `check` does not run the `pitest` task. The report is in build/reports/pitest/.
plugins {
    id("info.solidsoft.pitest")
}

val libs = the<VersionCatalogsExtension>().named("libs")

configure<PitestPluginExtension> {
    pitestVersion.set(libs.findVersion("pitest-tool").get().requiredVersion)
    junit5PluginVersion.set(libs.findVersion("pitest-junit5").get().requiredVersion)
    targetTests.set(listOf("de.seuhd.campuscoffee.*"))
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    failWhenNoMutations.set(false)
    // The task fails when the tests detect fewer than 85% of the mutants that they execute. The share of all mutants
    // would also count the code that only the system tests execute. Raise the value as the tests improve.
    testStrengthThreshold.set(85)
    threads.set(1)
    timeoutConstInMillis.set(30_000)
    jvmArgs.set(listOf("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "--sun-misc-unsafe-memory-access=allow"))
}
