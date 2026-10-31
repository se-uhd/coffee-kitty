plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // Compile scope rather than runtimeOnly, so that the IDE resolves in application.yaml the keys of a
    // `@ConfigurationProperties` class in the data module. The data module declares none yet.
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.webmvc)
    // The fixture loader logs via KotlinLogging.logger {} (a Kotlin facade over SLF4J).
    implementation(libs.kotlin.logging)

    // On the compile classpath, although no source imports it, because IntelliJ looks up the driver-class-name in
    // application.yaml only in compile dependencies.
    implementation(libs.postgresql)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.spring)
    testImplementation(libs.archunit)
}

springBoot {
    // Write META-INF/build-info.properties so that a BuildProperties bean exposes the version to OpenApiConfig. Omit
    // the build time, which would change the file, and with it the inputs of the tests, on every build.
    buildInfo {
        excludes.add("time")
    }
}

// Only the executable bootJar is used, so drop the plain library jar.
tasks.named("jar") {
    enabled = false
}
