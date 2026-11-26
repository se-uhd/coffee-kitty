// ADR-012 the models are checked against the code
plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
}

dependencies {
    implementation(libs.plantuml.mit)
    implementation(libs.alloy)
    testImplementation(project(":domain"))
    testImplementation(testFixtures(project(":domain")))
}

val models = layout.projectDirectory.dir("src/main/tla")
val charts = rootProject.layout.projectDirectory.dir("doc/statecharts")
val work = layout.buildDirectory.dir("statecharts")

fun JavaExec.runStatechart(mode: String) {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("de.seuhd.campuscoffee.models.statechart.StatechartKt")
    jvmArgs("-Djava.awt.headless=true")
    args(mode, models.asFile.path, charts.asFile.path, work.get().asFile.path)
    inputs.dir(models)
    inputs.file(rootProject.file("mise.toml"))
    outputs.dir(work)
}

val statecharts =
    tasks.register<JavaExec>("statecharts") {
        description = "Checks each TLA+ model with TLC and writes its chart to doc/statecharts."
        runStatechart("write")
        outputs.dir(charts)
    }

val checkStatecharts =
    tasks.register<JavaExec>("checkStatecharts") {
        description =
            "Fails when a rule of a TLA+ model breaks or when a chart in doc/statecharts differs from its model."
        runStatechart("check")
        inputs.dir(charts)
        mustRunAfter(statecharts)
    }

val alloyModels = layout.projectDirectory.dir("src/main/alloy")
val classDiagramDir = rootProject.layout.projectDirectory.dir("doc/class-diagrams")

fun JavaExec.runClassDiagram(mode: String) {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("de.seuhd.campuscoffee.models.alloy.ClassDiagramKt")
    jvmArgs("-Djava.awt.headless=true", "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn")
    args(mode, alloyModels.asFile.path, classDiagramDir.asFile.path)
    inputs.dir(alloyModels)
}

val classDiagrams =
    tasks.register<JavaExec>("classDiagrams") {
        description = "Writes the class diagram of each Alloy model to doc/class-diagrams."
        runClassDiagram("write")
        outputs.dir(classDiagramDir)
    }

val checkClassDiagrams =
    tasks.register<JavaExec>("checkClassDiagrams") {
        description = "Fails when a class diagram in doc/class-diagrams differs from its Alloy model."
        runClassDiagram("check")
        inputs.dir(classDiagramDir)
        // External: https://docs.gradle.org/current/userguide/incremental_build.html Gradle skips only a task with
        // outputs, so the check writes an empty file when it passes.
        val checked = layout.buildDirectory.file("class-diagrams/checked")
        outputs.file(checked)
        doLast { checked.get().asFile.writeText("") }
        mustRunAfter(classDiagrams)
    }

tasks.named("check") {
    dependsOn(checkStatecharts, checkClassDiagrams)
}

tasks.named<Test>("test") {
    inputs.dir(models)
    inputs.dir(alloyModels)
    inputs.file(rootProject.file("mise.toml"))
}
