package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Kotlin configuration for the modules: the Java toolchain and target, the Spring all-open plugin
// (so @Component/@Transactional classes can be proxied), ktlint formatting and linting, detekt with the custom
// campus-coffee-kdoc rules, and the Kotlin reflection library that Spring requires at runtime. The Java major version
// is sourced from the version catalog (libs `java`), the same entry the Java toolchain uses, so the two cannot
// diverge.
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion

kotlin {
    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
        // retain parameter names for Spring's @PathVariable/@RequestParam binding, as -parameters does for Java
        javaParameters.set(true)
    }
}

// Formatting follows ktlint's own `ktlint_official` code style (not IntelliJ's Kotlin coding conventions), which the
// root .editorconfig selects together with the line length. The plugin wires ktlintCheck into `check`, so a
// formatting violation fails the build.
configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// Run ktlint on the catalog's `ktlint-kotlin` compiler (gradle/libs.versions.toml explains the version). Without this
// block, the Spring dependency management would resolve ktlint's Kotlin artifacts to the build's Kotlin, which
// ktlint 1.8.0 cannot run on. Remove the block once ktlint runs on the build's `kotlin`, that is, once
// `gradle ktlintCheck` passes without it.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}

// A detekt 2.0 alpha only runs on the exact Kotlin it was built against. The Spring dependency management would
// resolve detekt's Kotlin artifacts to the build's Kotlin, so pin them to the catalog's `detekt-kotlin` instead.
configurations.matching { it.name.startsWith("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("detekt-kotlin").get().requiredVersion)
        }
    }
}

// Static analysis with detekt: The plugin wires its task into `check` and fails the build on findings by
// default. buildUponDefaultConfig layers our config on detekt's defaults, and config/detekt/detekt.yml enables
// the custom campus-coffee-kdoc rules. No module has a detekt-baseline.xml, the file of accepted findings that
// detekt reads by default, so every finding fails the build.
detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.from(rootProject.file("config/detekt/detekt.yml"))
}

// The `detekt` task scans main and test sources by default. Restrict it to the main source set, so that the KDoc rules
// skip the test sources. No detekt rule then checks the test sources.
tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin"))
}

dependencies {
    implementation(kotlin("reflect"))
    // The custom KDoc rules, which config/detekt/detekt.yml activates.
    detektPlugins(project(":detekt-rules"))
}
