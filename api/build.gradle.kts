plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.jacoco-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
}

dependencies {
    // Public signatures in this module contain domain types, such as `DtoMapper<User, UserDto>`. api() puts the
    // domain module on the compile classpath of every module that depends on the api module.
    api(project(":domain"))

    implementation(libs.spring.boot.starter.webmvc)
    // kotlin-logging wraps SLF4J for Kotlin. The classes that log, such as GlobalExceptionHandler, use it.
    implementation(libs.kotlin.logging)
    // springdoc generates the OpenAPI definition from the controllers and serves it with Swagger UI.
    // application.yaml turns both off, and the dev profile turns them on.
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    // Spring Security runs ActingUserAuthenticationFilter before the controllers (see SecurityConfig).
    implementation(libs.spring.boot.starter.security)
    // Spring MVC in Spring Boot 4 converts the DTOs from and to JSON with Jackson 3 (package `tools.jackson`).
    // Jackson 3 needs this Kotlin module to create a DTO through its constructor, to reject null for a non-null type,
    // and to apply default values. Spring Boot finds the module on the classpath and registers it.
    implementation(libs.jackson3.module.kotlin)

    // The mappers need the MapStruct annotations only at compile time. kapt runs the MapStruct processor, which
    // generates the implementations of the mappers.
    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)
}
