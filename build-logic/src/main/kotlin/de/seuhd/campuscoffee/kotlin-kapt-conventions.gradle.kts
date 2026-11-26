package de.seuhd.campuscoffee

plugins {
    kotlin("kapt")
}

// ADR-001, ADR-014
configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
}
