// Declare the Kotlin Gradle plugin once at the root (without applying it here) so a single classloader
// provides it to every subproject's convention plugins. Otherwise the Kotlin Gradle plugin warns that it was
// loaded multiple times in different subprojects.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
