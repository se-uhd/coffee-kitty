package de.seuhd.campuscoffee

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.process.CommandLineArgumentProvider

// Java configuration of the modules: the Java toolchain, the Spring Boot BOM, the test dependencies, and the test JVM.
// The Java toolchain and the Kotlin target in kotlin-conventions both read the `java` version from
// gradle/libs.versions.toml. mise.toml pins the same major version by hand.
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion
val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion

// project.group and project.version come from `group` and `version` in the root gradle.properties (see the comment
// there).

// Keep kotlin-stdlib and kotlin-reflect at the version of the Kotlin Gradle plugin (`kotlin` in
// gradle/libs.versions.toml). Without this line, the Spring Boot BOM would downgrade both libraries to the older Kotlin
// version that it manages.
extra["kotlin.version"] = libs.findVersion("kotlin").get().requiredVersion
// The Spring Boot BOM sets the version of every org.junit artifact from junit-jupiter.version. Set this property to the
// `junit` version from gradle/libs.versions.toml.
extra["junit-jupiter.version"] = libs.findVersion("junit").get().requiredVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

// A project property with the name of a BOM property overrides the version that the BOM manages. The `extra` entries
// above set two such properties, kotlin.version and junit-jupiter.version. The root gradle.properties can set more such
// properties in `<name>.version` lines.
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    // Test dependencies of every module. spring-boot-starter-test brings JUnit 6, Mockito, and AssertJ, and
    // mockito-kotlin adds Kotlin helpers for Mockito. Gradle needs the JUnit Platform launcher on the test runtime
    // classpath. The Spring Boot BOM manages the version of the launcher but does not add the dependency. Production
    // dependencies, such as spring-boot-starter-webmvc, are in the build files of the modules that use them.
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
    testImplementation(libs.findLibrary("mockito-kotlin").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

// The Mockito agent is the mockito-core jar. Resolve this jar without its dependencies in a separate configuration, and
// pass the jar to the test JVM as -javaagent (see below).
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
    jvmArgs(
        // Turn off class data sharing. With class data sharing on, the JVM prints "Sharing is only supported for boot
        // loader classes" when the Mockito agent appends to the boot class path.
        "-Xshare:off",
        // Silence the JDK 25 warnings about the deprecated sun.misc.Unsafe (JEP 498) and about restricted native
        // access, for example when Testcontainers loads JNA. A forked test JVM does not inherit these flags from the
        // Gradle daemon (see gradle.properties).
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED"
    )
    // The provider uses the local val `agentJar`, which wraps the configuration `mockitoAgent` in files(...). A
    // reference to `mockitoAgent` itself would keep the configuration cache from storing the task.
    val agentJar = files(mockitoAgent)
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf("-javaagent:${agentJar.singleFile.absolutePath}")
        }
    )
}
