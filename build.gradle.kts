// Declare the Kotlin Gradle plugin once at the root (without applying it here) so a single classloader
// provides it to every subproject's convention plugins. Otherwise the Kotlin Gradle plugin warns that it was
// loaded multiple times in different subprojects.
plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
}

// The subprojects get ktlint from their convention plugins. Applied here, ktlint also lints the root scripts
// build.gradle.kts and settings.gradle.kts, and the base plugin gives the root project the `check` task that runs it.
ktlint {
    version.set(libs.versions.ktlint.tool)
}

// The convention plugins in the included build build-logic belong to no subproject, so no subproject's check lints
// them.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"))
}
