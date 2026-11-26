plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.pitest-conventions")
    // ADR-005
    `java-test-fixtures`
}

dependencies {
    // ADR-002
    implementation(libs.spring.boot.core)
    implementation(libs.spring.tx)
    // ADR-009
    api(libs.jakarta.validation.api)
    runtimeOnly(libs.spring.boot.starter.validation)
}
