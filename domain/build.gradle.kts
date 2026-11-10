plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
}

dependencies {
    // @Service comes from spring-context, a dependency of spring-boot. @Transactional comes from spring-tx.
    implementation(libs.spring.boot.core)
    implementation(libs.spring.tx)
    // DataResetServiceImpl logs with kotlin-logging, which wraps SLF4J for Kotlin.
    implementation(libs.kotlin.logging)
}
