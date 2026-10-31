package de.seuhd.campuscoffee.api.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

/**
 * Web MVC configuration for the REST API: centralizes the base path and keeps responses JSON-only.
 *
 * Every controller in the `api.controller` package is served under [API_BASE_PATH], so the prefix is
 * declared once here instead of repeated on each `@RequestMapping`.
 */
@Configuration
class ApiWebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            API_BASE_PATH,
            HandlerTypePredicate.forBasePackage("de.seuhd.campuscoffee.api.controller")
        )
    }

    /**
     * Keeps the REST API JSON-only and its charset explicit.
     *
     * It drops every XML message converter, so that a client's `Accept` header cannot switch responses to XML.
     * Hibernate puts JAXB on the runtime classpath, so Spring registers its JAXB XML converter, which the method
     * drops along with any XML converter that a later dependency adds.
     *
     * Spring no longer adds a `charset` to JSON responses (RFC 8259 requires UTF-8 and defines no charset parameter),
     * so the `Content-Type` is a bare `application/json`. Clients that fall back to ISO-8859-1 when no charset is
     * declared then garble non-ASCII text (e.g., `Müller` renders as `MÃ¼ller`). The method therefore pins the charset
     * back onto the JSON converters, so that responses advertise `application/json;charset=UTF-8`.
     */
    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.configureMessageConvertersList { converters ->
            converters.removeIf { converter -> converter.supportedMediaTypes.any { it.subtype.endsWith("xml") } }
            converters
                .filterIsInstance<AbstractHttpMessageConverter<*>>()
                .filter { converter ->
                    converter.supportedMediaTypes.any {
                        it.subtype == "json" ||
                            it.subtype.endsWith("+json")
                    }
                }.forEach { converter ->
                    converter.supportedMediaTypes =
                        converter.supportedMediaTypes.map { MediaType(it.type, it.subtype, StandardCharsets.UTF_8) }
                }
        }
    }

    companion object {
        const val API_BASE_PATH = "/api"
    }
}
