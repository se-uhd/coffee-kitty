package de.seuhd.campuscoffee

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    jacoco
}

val libs = the<VersionCatalogsExtension>().named("libs")

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}
