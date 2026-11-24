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
// The first rule checks the coverage of all modules together. Its minimums are a few points below the current coverage.
// Raise them as the coverage grows, and never lower them to make a build pass. A ratio over all modules cannot detect
// a single untested function: The undo status (FR-9) once had no test at all, and the ratio stayed above its minimum.
//
// The second rule therefore requires that a test executes every function of a domain service and of a controller. It
// leaves out the functions that Kotlin generates for a lambda, such as the lambda of `KotlinLogging.logger {}`, which
// only names the logger and never runs.
val gateExclusions = listOf("**/Application*", "**/*MapperImpl.*")

val coverageGate = tasks.register<JacocoCoverageVerification>("coverageGate") {
    group = "verification"
    description =
        "Fails the build when the modules together cover less than 95% of the lines or 85% of the branches, or when " +
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
                minimum = "0.95".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
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
