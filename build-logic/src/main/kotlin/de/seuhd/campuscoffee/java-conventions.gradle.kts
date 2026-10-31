package de.seuhd.campuscoffee

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.process.CommandLineArgumentProvider

// Shared configuration for the library/application modules: the Java toolchain (also used by the
// Kotlin compilation), the Spring Boot BOM, and the test JVM args. The Java major version is sourced
// from the version catalog (libs `java`), the entry that the Kotlin target also reads, and mise.toml
// pins the same major by hand.
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion
val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion

// project.group and project.version come from the root gradle.properties (`group`/`version`). That version is the
// source of truth, and scripts/check-version-sync.sh, a CI step, checks that the latest CHANGELOG.md release header
// matches it.

// Keep kotlin-stdlib and kotlin-reflect at the version of the Kotlin Gradle plugin. The Boot BOM manages an older
// Kotlin and would otherwise downgrade them below the compiler.
extra["kotlin.version"] = libs.findVersion("kotlin").get().requiredVersion
// The Boot BOM manages every org.junit artifact through junit-jupiter.version, so set it to the catalog's JUnit.
extra["junit-jupiter.version"] = libs.findVersion("junit").get().requiredVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

// A project property named like a property of this BOM overrides the version that the BOM manages: the `extra` entries
// above (kotlin.version and junit-jupiter.version) or a `<name>.version` line in the root gradle.properties.
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    // Test stack shared by every module: JUnit 6, Mockito, and AssertJ through the starter, plus the
    // JUnit Platform launcher that Gradle needs on the test runtime classpath (the Spring Boot BOM
    // manages its version but does not add the dependency). Production dependencies that only some
    // modules use (e.g., spring-boot-starter-webmvc) are declared in those modules' build files.
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
    testImplementation(libs.findLibrary("mockito-kotlin").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

// Mockito's agent is the mockito-core jar itself. Resolve just that jar on a dedicated non-transitive configuration to
// pass it as -javaagent.
val mockitoAgent = configurations.create("mockitoAgent") {
    isCanBeConsumed = false
    isTransitive = false
}
dependencies {
    "mockitoAgent"(libs.findLibrary("mockito-core").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(
        // Turn off class data sharing, which otherwise prints "Sharing is only supported for boot loader classes"
        // once the Mockito agent appends to the boot class path.
        "-Xshare:off",
        // JDK 25: silence the deprecated sun.misc.Unsafe (JEP 498) and restricted-native-access warnings the forked
        // test JVM would otherwise print (e.g., through Testcontainers, which loads JNA). A forked test JVM does not
        // inherit the daemon flags from gradle.properties, so they are repeated here.
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED"
    )
    // The provider uses a local file collection instead of the script's `mockitoAgent`, because a reference to
    // the script would keep the configuration cache from storing the task.
    val agentJar = files(mockitoAgent)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${agentJar.singleFile.absolutePath}")
    })
}
