package de.seuhd.campuscoffee

import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

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
    // ADR-010
    targetClasses.set(listOf("de.seuhd.campuscoffee.domain.implementation.*"))
    excludedMethods.set(listOf("undoableUntil", "closesAt"))
    avoidCallsTo.set(setOf("kotlin.jvm.internal"))
    mutationThreshold.set(90)
    testStrengthThreshold.set(90)
    // ADR-014
    jvmArgs.set(listOf("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "--sun-misc-unsafe-memory-access=allow"))
}
