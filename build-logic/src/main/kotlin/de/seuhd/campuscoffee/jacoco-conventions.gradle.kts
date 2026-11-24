package de.seuhd.campuscoffee

import org.gradle.api.artifacts.VersionCatalogsExtension

// JaCoCo records the execution data of the tests of a module in build/jacoco/test.exec. The subproject `coverage`
// merges the execution data of all modules into one report, and its `coverageGate` task checks the coverage.
plugins {
    jacoco
}

val libs = the<VersionCatalogsExtension>().named("libs")

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}
