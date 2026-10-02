package de.seuhd.campuscoffee.api.openapi

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger configuration: the global API metadata. The version is set at runtime from the build (see
 * [apiVersionCustomizer]) rather than hard-coded in the annotation.
 */
@Configuration
@OpenAPIDefinition(
    info =
        Info(
            title = "CampusCoffeeConsumption API",
            description =
                "REST API of a coffee counter. It lists and adds users, reads a user's coffee count, and adds a " +
                    "coffee to the count."
        )
)
class OpenApiConfig {
    /**
     * Sets the OpenAPI document version from the build's [BuildProperties], falling back to "dev"
     * when the build info resource is absent. Keeps the version in one place: the Gradle build.
     *
     * @param buildProperties the build info provider, empty when the build info resource is absent
     */
    @Bean
    fun apiVersionCustomizer(buildProperties: ObjectProvider<BuildProperties>): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.info?.version = buildProperties.ifAvailable?.version ?: "dev"
        }
}
