package de.seuhd.campuscoffee

plugins {
    kotlin("kapt")
}

// ADR-001 kapt for MapStruct, ADR-014 a mapper fails to compile for an unmapped target property
configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
}
