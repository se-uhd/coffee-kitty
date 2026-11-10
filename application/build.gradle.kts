plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // No source in this module imports the data module. The data module is still a compile dependency, because
    // IntelliJ resolves the keys of a `@ConfigurationProperties` class in application.yaml only from compile
    // dependencies. The data module does not declare such a class yet.
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.webmvc)
    // FixtureStartupLoader logs through kotlin-logging, which wraps SLF4J in a Kotlin API.
    implementation(libs.kotlin.logging)

    // No source imports the PostgreSQL driver. The driver is still a compile dependency, because IntelliJ resolves
    // the driver-class-name in application.yaml only from compile dependencies.
    implementation(libs.postgresql)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.spring)
    testImplementation(libs.archunit)
}

springBoot {
    // Writes META-INF/build-info.properties. Spring Boot reads the file into a BuildProperties bean, and
    // OpenApiConfig takes the API version from that bean. The file leaves out the build time. A build time would
    // change the file on every build, and Gradle would then rerun the tests even when no code changed.
    buildInfo {
        excludes.add("time")
    }
}

// Disables the plain jar, because only the executable jar that bootJar builds is used.
tasks.named("jar") {
    enabled = false
}
