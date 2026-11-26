plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// ADR-023 a warning of the Kotlin compiler fails the build
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// ADR-001 a convention plugin applies a plugin by its id only from this classpath
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.pitest.gradle.plugin)
    implementation(libs.snakeyaml)

    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set(libs.versions.ktlint.tool)
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated-sources/") }
    }
}

// ADR-014 ktlint runs on the Kotlin of ktlint-kotlin
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
