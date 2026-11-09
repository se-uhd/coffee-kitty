plugins {
    id("de.seuhd.coffeekitty.java-conventions")
    id("de.seuhd.coffeekitty.kotlin-conventions")
}

dependencies {
    // The Spring Boot core brings spring-context for @Service, and spring-tx brings @Transactional.
    implementation(libs.spring.boot.core)
    implementation(libs.spring.tx)
    // DataResetServiceImpl logs via KotlinLogging.logger {} (a Kotlin facade over SLF4J).
    implementation(libs.kotlin.logging)
}
