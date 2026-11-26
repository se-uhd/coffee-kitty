package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// ADR-001 a convention plugin, ADR-014 kotlin-conventions would make detekt-rules depend on itself
plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion

kotlin {
    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
        // ADR-023 a warning of the Kotlin compiler fails the build
        allWarningsAsErrors.set(true)
    }
}

configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// ADR-014 ktlint runs on the Kotlin of ktlint-kotlin
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}

// External: https://detekt.dev/docs/gettingstarted/gradle/#gradle-runtime-dependencies detekt runs only on the
// Kotlin version that it was built with.
configurations.matching { it.name.startsWith("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("detekt-kotlin").get().requiredVersion)
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.from(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    detektPlugins(files(sourceSets["main"].output))
}

// ADR-023 all rules on the tests and the test fixtures as well
tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin", "src/test/kotlin", "src/testFixtures/kotlin"))
}

// ADR-023 the rules of type-resolution.yml with type resolution, in check as well
val typeResolved =
    tasks.withType<Detekt>().matching { it.name in setOf("detektMain", "detektTest", "detektTestFixtures") }
typeResolved.configureEach {
    config.setFrom(rootProject.file("config/detekt/type-resolution.yml"))
    buildUponDefaultConfig.set(false)
}
tasks.named("check") { dependsOn(typeResolved) }

// ADR-023 LineComment looks up the number of each pointer in the file names of doc/adr
tasks.withType<Detekt>().configureEach {
    inputs
        .files(rootProject.fileTree("doc/adr") { include("*.md") })
        .withPropertyName("adrs")
        .withPathSensitivity(PathSensitivity.NAME_ONLY)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // External: https://openjdk.org/jeps/498 JDK 25 warns when the Kotlin compiler and caffeine of detekt-test-utils
    // call sun.misc.Unsafe. A forked test JVM does not inherit these flags from the Gradle daemon (gradle.properties).
    jvmArgs("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED")
}
