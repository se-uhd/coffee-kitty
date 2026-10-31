package de.seuhd.campuscoffee

// kapt for the MapStruct annotation processor on the Kotlin mappers. Applied to the api and data
// modules, which add the processor via the `kapt(...)` configuration.
plugins {
    kotlin("kapt")
}

// Fail the build on a mapper target property that has neither a source nor `ignore = true`. MapStruct's default is
// only a warning, so a nullable property added to a domain model but not to its DTO would be set to null without
// notice, and an update would then store that null.
configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
}
