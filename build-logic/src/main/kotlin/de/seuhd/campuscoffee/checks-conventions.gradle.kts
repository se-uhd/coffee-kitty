package de.seuhd.campuscoffee

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    base
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

val libs = the<VersionCatalogsExtension>().named("libs")

configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint-tool").get().requiredVersion)
}

// ADR-014
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("ktlint-kotlin").get().requiredVersion)
        }
    }
}
val repository = layout.projectDirectory
val outsideTheRepository =
    listOf("**/build/**", "**/.gradle/**", ".git/**", ".claude/worktrees/**", "**/node_modules/**")

val markdownLimits =
    tasks.register<MarkdownLimitsTask>("markdownLimits") {
        group = "verification"
        description = "Fails when a Markdown file breaks a limit of config/markdown/limits.yml or no entry lists it."
        config.set(repository.file("config/markdown/limits.yml"))
        root.set(repository)
        markdownFiles.from(
            fileTree(repository) {
                include("**/*.md")
                exclude(outsideTheRepository + ".idea/**")
            }
        )
    }

configurations.matching { it.name.startsWith("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("detekt-kotlin").get().requiredVersion)
        }
    }
}

val detektComments =
    tasks.register<Detekt>("detektComments") {
        group = "verification"
        description = "Applies the comment rules of config/detekt/detekt.yml to the Kotlin files outside src/main."
        setSource(
            fileTree(repository) {
                include("**/*.kt", "**/*.kts")
                exclude(outsideTheRepository)
                exclude(listOf("domain", "api", "data", "application", "models").map { "$it/src/main/**" })
            }
        )
        config.setFrom(repository.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(false)
    }

tasks.named("check") {
    dependsOn(markdownLimits, detektComments)
}
