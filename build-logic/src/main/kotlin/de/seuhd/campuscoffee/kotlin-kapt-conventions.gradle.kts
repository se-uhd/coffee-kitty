package de.seuhd.campuscoffee

// kapt runs the MapStruct annotation processor on the Kotlin mappers. The api and data modules apply this convention
// plugin and add the processor to the `kapt` configuration.
plugins {
    kotlin("kapt")
}

// Fail the build when a target property of a mapper has neither a source nor `ignore = true`. By default, MapStruct
// only warns. With that default, a nullable property added to a domain model but not to its DTO would be set to null
// without notice, and an update would store that null.
configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
}
