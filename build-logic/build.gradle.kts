plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// Put the jars of the Gradle plugins that the convention plugins apply by id on the classpath of build-logic.
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
}

// Lint the convention plugins with the ktlint version of the subprojects. The root `check` task runs ktlintCheck of
// build-logic. The filter excludes the accessors that Gradle generates under build/ for the precompiled script plugins.
ktlint {
    version.set(libs.versions.ktlint.tool)
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated-sources/") }
    }
}
