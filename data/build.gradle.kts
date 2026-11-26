plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.kotlin-jpa-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
    // ADR-005
    `java-test-fixtures`
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    testFixturesImplementation(libs.spring.boot.core)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.testcontainers.postgresql)
}
