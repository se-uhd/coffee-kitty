pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// The only declaration of the subprojects' repositories. IntelliJ flags `repositoriesMode` and `repositories` as
// unstable, because Gradle marks them @Incubating, and there is no stable alternative for declaring repositories
// centrally. FAIL_ON_PROJECT_REPOS fails the build when a project declares its own repositories,
// which would otherwise replace these.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

// gradle/gradle-daemon-jvm.properties requires JDK 25 for the Gradle daemon. Gradle does not detect JDKs
// installed by mise, so if Gradle is started with another JDK, for example by IntelliJ, this plugin lets
// Gradle download a JDK 25 instead of failing.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "coffee-kitty"

include("domain", "api", "data", "application", "detekt-rules")
