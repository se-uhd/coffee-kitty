plugins {
    `kotlin-dsl`
}

// The convention plugins apply these Gradle plugins by id, so their jars must be on build-logic's
// classpath here.
dependencies {
    implementation(libs.dependency.management.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
}
