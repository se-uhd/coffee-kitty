pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "coffee-kitty"

include("domain", "api", "data", "application", "coverage", "detekt-rules")

// ADR-012 The models are optional, and gradle -Pmodels includes them.
if (providers.gradleProperty("models").isPresent) {
    include("models")
}
