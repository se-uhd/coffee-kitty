package de.seuhd.campuscoffee

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.process.CommandLineArgumentProvider

// ADR-001 the Java toolchain and the Spring Boot BOM of every module
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion
val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion

// External: https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/ A project property with
// the name of a version property of the Spring Boot BOM, such as junit-jupiter.version for every org.junit artifact,
// overrides the version that the BOM manages.
extra["kotlin.version"] = libs.findVersion("kotlin").get().requiredVersion
extra["junit-jupiter.version"] = libs.findVersion("junit").get().requiredVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
    testImplementation(libs.findLibrary("mockito-kotlin").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

val mockitoAgent =
    configurations.create("mockitoAgent") {
        isCanBeConsumed = false
        isTransitive = false
    }
dependencies {
    "mockitoAgent"(libs.findLibrary("mockito-core").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        // ADR-014 the assertion and a stack trace that IntelliJ IDEA links
        exceptionFormat = TestExceptionFormat.FULL
    }
    jvmArgs(
        "-Duser.timezone=Asia/Tokyo",
        // ADR-014 class data sharing off for the Mockito agent
        "-Xshare:off",
        // External: https://openjdk.org/jeps/498 JDK 25 warns about calls to sun.misc.Unsafe, and about native access
        // (JEP 472) such as when Testcontainers loads JNA. A forked test JVM does not inherit the flags of the daemon.
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED"
    )
    val agentJar = files(mockitoAgent)
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf("-javaagent:${agentJar.singleFile.absolutePath}")
        }
    )
}
