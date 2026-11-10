// The root project declares the Kotlin Gradle plugin without applying it. Gradle then loads the plugin once, in the
// classloader of the root project, which the subprojects inherit. Without this declaration, the Kotlin Gradle plugin
// warns that it was loaded multiple times in different subprojects.
plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
}

// The subprojects apply ktlint through their convention plugins. In the root project, ktlint lints build.gradle.kts and
// settings.gradle.kts, and the `base` plugin adds the `check` task that runs ktlint.
ktlint {
    version.set(libs.versions.ktlint.tool)
}

// No subproject lints the convention plugins, because they belong to the included build build-logic. The root `check`
// task runs the ktlintCheck task of build-logic.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"))
}
