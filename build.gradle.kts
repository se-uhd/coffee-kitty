// The root project declares the Kotlin Gradle plugin without applying it. Gradle then loads the plugin once, in the
// classloader of the root project, which the subprojects inherit. Without this declaration, the Kotlin Gradle plugin
// warns that it was loaded multiple times in different subprojects.
plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
}

// The subprojects with Kotlin code apply ktlint through their convention plugins. In the root project, ktlint lints
// build.gradle.kts and settings.gradle.kts, and the `base` plugin adds the `check` task that runs ktlint.
ktlint {
    version.set(libs.versions.ktlint.tool)
}

// The configuration `cpd` holds the command-line interface (CLI) of PMD. The `cpd` task runs the copy-paste detector
// (CPD) of PMD on the Kotlin production code. CPD compares sequences of tokens and leaves out whitespace and comments.
//
// The task fails at a duplicate of 70 tokens. Measured on 2026-11-23 and again on 2026-11-25: The largest duplicate in
// the production code has 60 tokens, the `@Mapping` annotations that two MapStruct mappers share. A limit of 60 or
// lower reports these annotations, which cannot be extracted. At 70 tokens, CPD reports a copy of a domain function
// once the copy has 10 to 16 source lines. At 100 tokens, it reports such a copy only at 13 to 17 lines.
// doc/adr/010-thresholds-of-the-build.md has the counts for each limit.
val cpd =
    configurations.create("cpd") {
        isCanBeConsumed = false
    }
dependencies {
    cpd(libs.pmd.cli)
    cpd(libs.pmd.kotlin)
}

tasks.register<JavaExec>("cpd") {
    group = "verification"
    description = "Fails when the production code contains a duplicate of at least 70 tokens (PMD CPD)."
    classpath = cpd
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args(
        listOf("cpd", "--language", "kotlin", "--minimum-tokens", "70") +
            listOf("domain", "api", "data", "application").flatMap { listOf("--dir", "$it/src/main/kotlin") }
    )
}

// No subproject lints the convention plugins, because they belong to the included build build-logic. The root `check`
// task runs the ktlintCheck task of build-logic.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"), "cpd")
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
