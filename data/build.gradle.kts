plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.kotlin-jpa-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // MapStruct is compile-only for the Kotlin mappers. kapt runs the processor that generates the implementations.
    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    testImplementation(libs.testcontainers.postgresql)
}
