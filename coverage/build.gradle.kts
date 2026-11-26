plugins {
    base
    jacoco
    id("jacoco-report-aggregation")
    id("io.spring.dependency-management")
}

// ADR-014 the BOM gives the versions of the dependencies of the modules
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
    // External: https://docs.gradle.org/current/userguide/validation_problems.html#implicit_dependency The source
    // directories of the application include application/build/bootBuildInfo, which bootBuildInfo writes.
    dependsOn(":application:bootBuildInfo")
}

// ADR-010 the classes that the coverage gate leaves out
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
            // ADR-010 the functions that the second rule of the gate leaves out
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
