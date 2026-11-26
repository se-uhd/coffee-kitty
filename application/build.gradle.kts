plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // ADR-014
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.kotlin.logging)

    // ADR-014
    implementation(libs.postgresql)

    // ADR-005
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":data")))
    // ADR-007
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
