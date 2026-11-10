pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// The repositories of all subprojects, declared here and nowhere else. FAIL_ON_PROJECT_REPOS fails the build when a
// build script declares repositories, because Gradle would use those in place of the repositories below. IntelliJ
// marks `repositoriesMode` and `repositories` as unstable, because Gradle annotates them with @Incubating. Gradle has
// no stable alternative.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

// gradle/gradle-daemon-jvm.properties requires JDK 25 for the Gradle daemon. Gradle does not detect a JDK that mise
// installed. When IntelliJ starts Gradle with another JDK, Gradle may not find a JDK 25. It then downloads a JDK 25
// from the URLs in that file. `gradle updateDaemonJvm` needs this plugin to write the URLs and fails without it. The
// build itself does not use the plugin, because Gradle uses the daemon JDK as the Java toolchain.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "coffee-kitty"

include("domain", "api", "data", "application", "detekt-rules")
