package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Kotlin configuration of the modules: the Java toolchain and the JVM target, the Spring all-open plugin, ktlint,
// detekt with the custom KDoc rules of campus-coffee-kdoc, and the Kotlin reflection library that Spring needs at
// runtime. The all-open plugin opens the classes annotated with @Component or @Transactional, which lets Spring create
// proxies of these classes. The toolchain and the JVM target use the `java` version from gradle/libs.versions.toml.
// java-conventions reads the same entry.
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
        // Write the parameter names into the class files (for Java, the javac flag -parameters does the same). Spring
        // binds @PathVariable and @RequestParam parameters by these names.
        javaParameters.set(true)
    }
}

// ktlint checks the code against the code style `ktlint_official` (not the Kotlin coding conventions of IntelliJ) and
// the line length, which the root .editorconfig sets. The ktlint plugin makes `check` depend on ktlintCheck, and a
// formatting violation fails the build.
configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// Run ktlint on the `ktlint-kotlin` compiler from gradle/libs.versions.toml, which explains the version. Without this
// block, the Spring dependency management would resolve the Kotlin artifacts of ktlint to the `kotlin` version of the
// build, and ktlint 1.8.0 fails on that version. Remove the block when `gradle ktlintCheck` passes without it.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}

// Run detekt on the `detekt-kotlin` compiler from gradle/libs.versions.toml. A detekt 2.0 alpha runs only on the Kotlin
// version that it was built with. Without this block, the Spring dependency management would resolve the Kotlin
// artifacts of detekt to the `kotlin` version of the build.
configurations.matching { it.name.startsWith("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("detekt-kotlin").get().requiredVersion)
        }
    }
}

// The detekt plugin makes `check` depend on the `detekt` task, which fails the build on any finding. detekt would skip
// the findings listed in a detekt-baseline.xml, but no module has this file. buildUponDefaultConfig applies
// config/detekt/detekt.yml on top of the default configuration of detekt. That file enables the custom KDoc rules.
detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.from(rootProject.file("config/detekt/detekt.yml"))
}

// The `detekt` task checks the main and the test sources by default. Restrict the task to src/main/kotlin, so that the
// test sources need no KDoc. No other detekt rule checks the test sources either.
tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin"))
}

dependencies {
    implementation(kotlin("reflect"))
    // The custom KDoc rules, which config/detekt/detekt.yml enables.
    detektPlugins(project(":detekt-rules"))
}
