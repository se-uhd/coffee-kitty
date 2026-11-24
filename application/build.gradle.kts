plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":api"))
    // No source in this module imports the data module, and the data module does not declare a
    // `@ConfigurationProperties` class yet. The data module is still a compile dependency, because IntelliJ resolves
    // the keys of such a class in application.yaml only from compile dependencies.
    implementation(project(":data"))

    implementation(libs.spring.boot.starter.webmvc)
    // FixtureStartupLoader and BootstrapAdminLoader log through kotlin-logging, which wraps SLF4J in a Kotlin API.
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
    // The unit tests of the security classes of the api module use the types of Spring Security.
    testImplementation(libs.spring.boot.starter.security)
}

springBoot {
    // Writes META-INF/build-info.properties. Spring Boot reads the file into a BuildProperties bean, and
    // OpenApiConfig takes the API version from that bean. The file leaves out the build time. A build time would
    // change the file on every build, and Gradle would then rerun the tests even when no code changed.
    buildInfo {
        excludes.add("time")
    }
}

// Disables the plain jar, because the project needs only the executable jar that bootJar builds.
tasks.named("jar") {
    enabled = false
}
