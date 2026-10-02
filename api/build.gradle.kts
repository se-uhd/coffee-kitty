plugins {
    id("de.seuhd.campuscoffee.java-conventions")
    id("de.seuhd.campuscoffee.kotlin-conventions")
    id("de.seuhd.campuscoffee.kotlin-kapt-conventions")
}

dependencies {
    // api re-exposes domain types in its public signatures (e.g., `DtoMapper<User, UserDto>`).
    api(project(":domain"))

    implementation(libs.spring.boot.starter.web)
    // The exception handler logs via KotlinLogging.logger {} (a Kotlin facade over SLF4J).
    implementation(libs.kotlin.logging)
    // Swagger UI and the OpenAPI document, which the dev profile serves.
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.validation)
    // Jackson 3 Kotlin module: Spring MVC in Boot 4 reads and writes the DTOs with Jackson 3 (`tools.jackson`), which
    // needs it for the Kotlin data classes (e.g., constructor binding, nullability, and defaults), and Spring Boot
    // auto-registers it.
    implementation(libs.jackson3.module.kotlin)

    // MapStruct is compile-only for the Kotlin mappers; kapt runs the processor that generates the impls.
    compileOnly(libs.mapstruct)
    kapt(libs.mapstruct.processor)
}
