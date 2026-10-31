package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Build configuration for the :detekt-rules tooling module, which holds the custom detekt rules that kotlin-conventions
// loads via detektPlugins. It uses its own convention (not kotlin-conventions) only to avoid a self-referential
// detektPlugins(project(":detekt-rules")) dependency. The KDoc rules still apply to the rule sources, because detekt
// loads the rules from this module's own compiled output. Test sources are exempt, matching the main modules.
plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    }
}

configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// Run ktlint on the catalog's `ktlint-kotlin` compiler, as in kotlin-conventions.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}

// Run detekt on the catalog's `detekt-kotlin` compiler, as in kotlin-conventions: a detekt 2.0 alpha only runs on
// the exact Kotlin it was built against.
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

// Load the custom rules from this module's own compiled output (no self-referential project
// dependency), so they run against the rule sources themselves.
dependencies {
    detektPlugins(files(sourceSets["main"].output))
}

// Documentation rules apply to production code only; restrict to the main source set.
tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // JDK 25: the forked test JVM loads detekt-test-utils (kotlin-compiler-embeddable, caffeine), which call
    // the deprecated sun.misc.Unsafe; it does not inherit the daemon's flags, so set them here too.
    jvmArgs("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED")
}
