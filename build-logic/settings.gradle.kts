// The repositories of build-logic, declared here and nowhere else. FAIL_ON_PROJECT_REPOS fails the build when
// build-logic/build.gradle.kts declares repositories (see the root settings.gradle.kts).
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
