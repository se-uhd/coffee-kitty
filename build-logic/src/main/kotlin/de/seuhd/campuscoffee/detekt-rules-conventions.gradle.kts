package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Build configuration of the subproject :detekt-rules, which holds the custom detekt rules. kotlin-conventions loads
// these rules with detektPlugins(project(":detekt-rules")). :detekt-rules applies this convention plugin instead of
// kotlin-conventions, because in :detekt-rules that line would make the subproject depend on itself. detekt still
// checks the rule sources with the KDoc rules (see the dependencies block below).
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
    }
}

configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// Run ktlint on the `ktlint-kotlin` compiler from gradle/libs.versions.toml, which explains the version.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}

// Run detekt on the `detekt-kotlin` compiler from gradle/libs.versions.toml. A detekt 2.0 alpha runs only on the Kotlin
// version that it was built with.
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

// Load the custom rules from the output of the main source set of :detekt-rules. detekt then checks the rule sources
// with the rules that these sources define.
dependencies {
    detektPlugins(files(sourceSets["main"].output))
}

// Run the `detekt` task on src/main/kotlin only, so that the test sources need no KDoc. No other detekt rule checks the
// test sources either.
tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Pass the JDK 25 flags of the Gradle daemon (see gradle.properties) to the forked test JVM, which does not inherit
    // the daemon flags. The tests load kotlin-compiler-embeddable and caffeine through detekt-test-utils, and both
    // libraries call the deprecated sun.misc.Unsafe.
    jvmArgs("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED")
}
