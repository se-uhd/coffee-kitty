plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.kotlin-jpa-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
    // Adds the source set src/testFixtures, which holds DatabaseCleaner. The tests of this module use it, and the
    // application module adds it with testImplementation(testFixtures(project(":data"))). The production jar does not
    // contain it.
    `java-test-fixtures`
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // The mappers need the MapStruct annotations only at compile time. kapt runs the MapStruct processor, which
    // generates the mapper implementations.
    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    // DatabaseCleaner in src/testFixtures is a Spring bean (@Component).
    testFixturesImplementation(libs.spring.boot.core)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.testcontainers.postgresql)
}
