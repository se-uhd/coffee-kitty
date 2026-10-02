package de.seuhd.campuscoffee

// kapt for the MapStruct annotation processor on the Kotlin mappers. Applied to the api and data
// modules, which add the processor via the `kapt(...)` configuration.
plugins {
    kotlin("kapt")
}

// Fail the build on a mapper target property that has neither a source nor `ignore = true`. MapStruct's default
// is a warning, so a new nullable server-owned field on a domain model or entity was mapped to null without
// notice, and an update path then stored that null.
configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
}
