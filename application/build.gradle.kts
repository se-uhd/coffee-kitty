import dev.detekt.gradle.Detekt

plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // ADR-014 a compile dependency, so that the IDE resolves the keys of application.yaml
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.kotlin.logging)

    // ADR-014 a compile dependency, so that the IDE resolves driver-class-name
    implementation(libs.postgresql)

    // ADR-005 the test fixtures and the database cleaner are test code
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":data")))
    // ADR-007 the tests read the JSON of a response into their own classes
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.spring)
    testImplementation(libs.archunit)
}

springBoot {
    // External: https://docs.spring.io/spring-boot/gradle-plugin/integrating-with-actuator.html With the build time,
    // bootBuildInfo is never up-to-date, and Gradle reruns the tests on every build.
    buildInfo {
        excludes.add("time")
    }
}

tasks.named("jar") {
    enabled = false
}

// ADR-023 Without the plain jar, detektTest finds the internal declarations in the classes of the main source set.
tasks.named<Detekt>("detektTest") {
    friendPaths.setFrom(sourceSets.main.map { it.output.classesDirs })
}
