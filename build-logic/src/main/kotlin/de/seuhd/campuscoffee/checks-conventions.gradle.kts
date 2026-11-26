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

// ADR-014 ktlint runs on the Kotlin of ktlint-kotlin
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

val modules = listOf("domain", "api", "data", "application", "models", "coverage", "detekt-rules")

// ADR-023 the modules check their own sources, and this task checks the rest of the Kotlin files
val detektBuildLogic =
    tasks.register<Detekt>("detektBuildLogic") {
        group = "verification"
        description = "Applies detekt with config/detekt/detekt.yml to build-logic and the Gradle scripts."
        setSource(
            listOf(
                fileTree(repository.dir("build-logic")) {
                    include("**/*.kt", "**/*.kts")
                    exclude("**/build/**", "**/.gradle/**")
                },
                fileTree(repository) { include("*.kts") }
            ) + modules.map { module -> fileTree(repository.dir(module)) { include("*.kts") } }
        )
        config.setFrom(repository.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        // ADR-023 LineComment looks up the number of each pointer in the file names of doc/adr
        inputs
            .files(fileTree(repository.dir("doc/adr")) { include("*.md") })
            .withPropertyName("adrs")
            .withPathSensitivity(PathSensitivity.NAME_ONLY)
    }

val rules = fileTree(repository) { include(".claude/rules/*.md") }

tasks.register<RuleIndexTask>("ruleIndex") {
    group = "documentation"
    description = "Writes the lists of the rule files in CLAUDE.md and AGENTS.md from the front matter of the rules."
    ruleFiles.from(rules)
    config.set(repository.file("config/markdown/limits.yml"))
    root.set(repository)
    verify.set(false)
}

val ruleIndexCheck =
    tasks.register<RuleIndexTask>("ruleIndexCheck") {
        group = "verification"
        description = "Fails when a list of the rule files in CLAUDE.md or AGENTS.md differs from the rules."
        ruleFiles.from(rules)
        config.set(repository.file("config/markdown/limits.yml"))
        root.set(repository)
        verify.set(true)
    }

tasks.named("check") {
    dependsOn(markdownLimits, detektBuildLogic, ruleIndexCheck)
}
