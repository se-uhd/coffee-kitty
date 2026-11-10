plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// The convention plugins apply these Gradle plugins by id, so their jars must be on build-logic's
// classpath here.
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
}

// Lint the convention plugins with the same ktlint version as the subprojects. The root build.gradle.kts runs this
// check as part of `gradle check`. The exclusion skips the accessors that Gradle generates for the precompiled script
// plugins under build/.
ktlint {
    version.set(libs.versions.ktlint.tool)
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated-sources/") }
    }
}
