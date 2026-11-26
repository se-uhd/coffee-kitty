plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// ADR-001
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.pitest.gradle.plugin)
    implementation(libs.snakeyaml)
}

ktlint {
    version.set(libs.versions.ktlint.tool)
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated-sources/") }
    }
}

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
