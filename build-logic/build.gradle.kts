plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// The convention plugins apply these Gradle plugins by id, and the classpath of build-logic must contain their jars.
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.pitest.gradle.plugin)
}

// Lint the convention plugins with the ktlint version of the subprojects. The root `check` task runs the ktlintCheck
// task of build-logic. The filter excludes the accessors that Gradle generates under build/ for the precompiled script
// plugins.
ktlint {
    version.set(libs.versions.ktlint.tool)
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated-sources/") }
    }
}

// Run ktlint on the `ktlint-kotlin` compiler from gradle/libs.versions.toml, as the convention plugins do. ktlint 1.8.0
// otherwise runs on the Kotlin 2.2.21 that it ships, which prints sun.misc.Unsafe warnings on JDK 25.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    val ktlintKotlin =
        libs.versions.ktlint.kotlin
            .get()
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(ktlintKotlin)
        }
    }
}
