package de.seuhd.campuscoffee.api.openapi

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The configuration of the OpenAPI definition. `@OpenAPIDefinition` sets the title and the description of the API, and
 * [apiVersionCustomizer] sets the version at runtime from the build.
 */
@Configuration
@OpenAPIDefinition(
    info =
        Info(
            title = "Coffee Kitty API",
            description =
                "The REST API of a coffee counter. It lists, reads, and creates users. For a user, it reads the " +
                    "coffee count, adds a coffee, undoes the most recent coffee within the grace period (5 minutes " +
                    "by default), and reads the undo status. " +
                    "An admin can correct a coffee count with a note and list the corrections of a user."
        )
)
class OpenApiConfig {
    /**
     * Sets the version of the OpenAPI definition to the version in `META-INF/build-info.properties`, or to "dev" when
     * the file does not exist. Spring Boot reads the file into [BuildProperties]. The Gradle build of the application
     * module writes the file with the project version from `gradle.properties`.
     *
     * @param buildProperties the provider of [BuildProperties], which is empty when the file does not exist
     */
    @Bean
    fun apiVersionCustomizer(buildProperties: ObjectProvider<BuildProperties>): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.info?.version = buildProperties.ifAvailable?.version ?: "dev"
        }
}
