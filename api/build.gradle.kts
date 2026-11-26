plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
}

dependencies {
    api(project(":domain"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.kotlin.logging)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.security)
    // ADR-008 the Kotlin module of Jackson rejects a body without a required field
    implementation(libs.jackson3.module.kotlin)

    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)
}
