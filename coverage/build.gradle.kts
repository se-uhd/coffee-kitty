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

// The coverageGate task leaves out the Application class and the generated mappers. The minimums below are those of the
// coffee app of SE@UHD. Raise them as the coverage grows, and never lower them to make a build pass.
val gateExclusions = listOf("**/Application*", "**/*MapperImpl.*")

val coverageGate = tasks.register<JacocoCoverageVerification>("coverageGate") {
    group = "verification"
    description = "Fails the build when the modules together cover less than 90% of the lines or 80% of the branches."
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
    }
}

tasks.named("check") {
    dependsOn(aggregateReport, coverageGate)
}
