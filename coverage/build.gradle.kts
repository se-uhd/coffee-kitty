plugins {
    base
    jacoco
    id("jacoco-report-aggregation")
    alias(libs.plugins.dependency.management)
}

// The plugin jacoco-report-aggregation resolves the dependencies of the modules. The Spring Boot BOM sets the versions
// of these dependencies.
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencies {
    jacocoAggregation(project(":domain"))
    jacocoAggregation(project(":api"))
    jacocoAggregation(project(":data"))
    jacocoAggregation(project(":application"))
}

reporting {
    reports {
        create<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName.set("test")
        }
    }
}

val aggregateReport = tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required.set(true)
    }
}

// The coverageGate task leaves out the Application class and the generated mappers. It has two rules.
//
// The first rule checks the coverage of all modules together: at least 90% of the lines and 80% of the branches. The
// minimums are on purpose several points below the measured coverage. A minimum close to the measured value makes
// developers write tests only to reach the number, and such tests check implementation details. Measured on 2026-11-25
// with the exclusions of the gate: The tests cover 699 of 719 lines (97.2%) and 142 of 166 branches (85.5%). Do not
// raise the minimums when the coverage grows (doc/adr/010-thresholds-of-the-build.md). A ratio over all modules cannot
// detect a single untested function: The undo status (FR-9) once had no test at all, and the ratio stayed above its
// minimum.
//
// The second rule therefore requires that a test executes every function of a domain service and of a controller. It
// leaves out the functions that Kotlin generates for a lambda, such as the lambda of `KotlinLogging.logger {}`, which
// only names the logger and never runs.
val gateExclusions = listOf("**/Application*", "**/*MapperImpl.*")

val coverageGate = tasks.register<JacocoCoverageVerification>("coverageGate") {
    group = "verification"
    description =
        "Fails the build when the modules together cover less than 90% of the lines or 80% of the branches, or when " +
        "no test executes a function of a domain service or a controller."
    executionData.from(aggregateReport.map { it.executionData })
    sourceDirectories.from(aggregateReport.map { it.sourceDirectories })
    classDirectories.from(aggregateReport.map { it.classDirectories.asFileTree.matching { exclude(gateExclusions) } })
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "METHOD"
            includes =
                listOf(
                    "de.seuhd.campuscoffee.domain.implementation.*",
                    "de.seuhd.campuscoffee.api.controller.*"
                )
            // Until 2026-12-08, the rule leaves out the three functions of the undo status (FR-9). Their tests are the
            // reference solution of assignment 7, which the repository does not contain before that date. Delete the
            // three patterns when the tests are on the main branch.
            excludes = listOf("*lambda*", "*.undoableUntil(*", "*.closesAt(*", "*.undoStatus(*")
            limit {
                counter = "LINE"
                value = "COVEREDCOUNT"
                minimum = "1".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(aggregateReport, coverageGate)
}
