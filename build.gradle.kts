// ADR-001, ADR-013
plugins {
    base
    id("de.seuhd.campuscoffee.checks-conventions")
}

// ADR-010
val cpd =
    configurations.create("cpd") {
        isCanBeConsumed = false
    }
dependencies {
    cpd(libs.pmd.cli)
    cpd(libs.pmd.kotlin)
    detektPlugins(project(":detekt-rules"))
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

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"), "cpd")
}
