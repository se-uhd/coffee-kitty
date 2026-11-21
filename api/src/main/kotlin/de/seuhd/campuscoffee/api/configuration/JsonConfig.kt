package de.seuhd.campuscoffee.api.configuration

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.type.LogicalType

/**
 * Configuration of the JSON mapper with which Spring MVC reads the request bodies. `application.yaml` holds the
 * settings of the JSON mapper that Spring Boot offers as properties under `spring.jackson`, and this class holds the
 * setting that has no property.
 */
@Configuration
class JsonConfig {
    /**
     * Returns the customizer that makes Jackson reject a JSON number or a JSON boolean for a `String` field, so that
     * the request gets a 400 with the error code `BadRequest`. By default, Jackson reads such a value as its text, and
     * a request with `"loginName": 12345` would create a user with the login name "12345". Jackson still reads a JSON
     * string and `null` for a `String` field.
     *
     * Jackson sets this rule in the coercion configuration of the target type, for which Spring Boot has no property
     * under `spring.jackson`. Spring Boot applies every [JsonMapperBuilderCustomizer] bean to the builder of its JSON
     * mapper.
     */
    @Bean
    fun strictStringCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.withCoercionConfig(LogicalType.Textual) { coercions ->
                coercions
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
            }
        }
}
