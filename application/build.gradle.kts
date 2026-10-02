plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // Compile-scoped (not runtimeOnly): A `@ConfigurationProperties` class that the data module declares is then
    // on this module's compile classpath, and the IDE resolves its keys in application.yaml.
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.web)
    // The fixture loader logs via KotlinLogging.logger {} (a Kotlin facade over SLF4J).
    implementation(libs.kotlin.logging)

    // The JDBC driver reaches the runtime classpath transitively via data, but declaring it on the
    // deployable module makes the runtime dependency explicit and lets the IDE resolve the
    // driver-class-name in application.yaml.
    runtimeOnly(libs.postgresql)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.spring)
    testImplementation(libs.archunit)
}

springBoot {
    // Write META-INF/build-info.properties so a BuildProperties bean exposes the version at runtime
    // (consumed by OpenApiConfig), keeping the version sourced from the build.
    buildInfo()
}

// Only the executable bootJar is consumed; drop the redundant plain library jar.
tasks.named("jar") {
    enabled = false
}
