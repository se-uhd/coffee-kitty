plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.pitest-conventions")
    // ADR-005 the test fixtures are test code
    `java-test-fixtures`
}

dependencies {
    // ADR-002 the Spring annotations, without Spring MVC or JPA
    implementation(libs.spring.boot.core)
    implementation(libs.spring.tx)
    // ADR-009 the value rules are constraints on the domain models
    api(libs.jakarta.validation.api)
    runtimeOnly(libs.spring.boot.starter.validation)
}
