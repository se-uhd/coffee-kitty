plugins {
    id("de.seuhd.campuscoffee.detekt-rules-conventions")
}

dependencies {
    // detekt-api brings the rule base classes and (via apiElements) the Kotlin compiler PSI that the
    // rules visit. detekt provides it at runtime, so compileOnly is enough for the main code.
    compileOnly(libs.detekt.api)

    // The tests run each rule against a compiled snippet: detekt-api for the rule types, and
    // detekt-test-utils to turn a Kotlin string into a KtFile (see RuleTestSupport).
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test.utils)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
