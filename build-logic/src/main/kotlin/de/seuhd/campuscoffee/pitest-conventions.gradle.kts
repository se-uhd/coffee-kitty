package de.seuhd.campuscoffee

import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

// PIT changes the code of a module, e.g., it negates a condition, and runs the tests of the module against each changed
// version (mutant). Only the domain module applies this plugin, because its unit tests run without Spring and
// PostgreSQL. `check` does not run the `pitest` task, and CI runs it in a step of its own. The report is in
// build/reports/pitest/.
//
// PIT changes only the classes in domain/.../implementation/, which hold the business logic. The unit tests of the
// domain module must detect the mutants of these classes. The models are data classes without logic, and the system
// tests of the application module execute them.
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
    targetClasses.set(listOf("de.seuhd.campuscoffee.domain.implementation.*"))
    // The task fails when the tests detect fewer than 90% of all mutants (mutationThreshold). This share also counts
    // the mutants in code that no unit test executes, so a domain function without a unit test lowers it. The test
    // strength counts only the mutants that a test executes and cannot detect such a function. Raise both thresholds as
    // the tests improve.
    // Until 2026-12-08, PIT leaves out the two domain functions of the undo status (FR-9). Their tests are the
    // reference solution of assignment 7, which the repository does not contain before that date. Delete this line
    // when the tests are on the main branch.
    excludedMethods.set(listOf("undoableUntil", "closesAt"))
    // PIT does not remove a call to kotlin.jvm.internal, such as a null check that the Kotlin compiler generates for a
    // platform type. No test can detect such a mutant, because the value is never null.
    avoidCallsTo.set(setOf("kotlin.jvm.internal"))
    mutationThreshold.set(90)
    testStrengthThreshold.set(90)
    threads.set(1)
    timeoutConstInMillis.set(30_000)
    jvmArgs.set(listOf("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "--sun-misc-unsafe-memory-access=allow"))
}
