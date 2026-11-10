plugins {
    id("de.seuhd.campuscoffee.detekt-rules-conventions")
}

dependencies {
    // detekt-api brings the base classes of the rules. The `api` dependencies of detekt-api add the PSI classes of the
    // Kotlin compiler that the rules visit. compileOnly is enough for the main code, because detekt provides detekt-api
    // at runtime.
    compileOnly(libs.detekt.api)

    // The tests run each rule on a Kotlin snippet. They need detekt-api for the rule types and detekt-test-utils to
    // compile a snippet into a KtFile (see RuleTestSupport.kt).
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test.utils)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
