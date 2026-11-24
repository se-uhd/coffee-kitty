plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.pitest-conventions")
    // Adds the source set src/testFixtures, which holds TestFixtures. The tests of this module use it, and the data and
    // application modules add it with testImplementation(testFixtures(project(":domain"))). The production jar does not
    // contain it.
    `java-test-fixtures`
}

dependencies {
    // @Service comes from spring-context, a dependency of spring-boot. @Transactional comes from spring-tx.
    implementation(libs.spring.boot.core)
    implementation(libs.spring.tx)
}
