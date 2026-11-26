package de.seuhd.campuscoffee.api.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

/**
 * The Spring MVC configuration of the REST API. It adds the prefix [API_BASE_PATH] to the path of every controller in
 * the package `api.controller`, and the controllers leave the prefix out of their `@RequestMapping`.
 * [configureMessageConverters] restricts the request and response bodies to JSON, with responses in UTF-8.
 */
@Configuration
class ApiWebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            API_BASE_PATH,
            HandlerTypePredicate.forBasePackage(CONTROLLER_PACKAGE)
        )
    }

    /**
     * Removes the XML and YAML message converters, which Spring registers because Hibernate and springdoc put JAXB and
     * the YAML module of Jackson on the classpath. Gives the JSON converters the default charset UTF-8, which Spring
     * adds to the media type of `produces`, because a client that assumes ISO-8859-1 decodes `Müller` as `MÃ¼ller`.
     */
    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.configureMessageConvertersList { converters ->
            converters.removeIf { converter ->
                converter.supportedMediaTypes.any { it.subtype.endsWith("xml") || it.subtype.endsWith("yaml") }
            }
            converters
                .filterIsInstance<AbstractHttpMessageConverter<*>>()
                .filter { converter ->
                    converter.supportedMediaTypes.any {
                        it.subtype == "json" ||
                            it.subtype.endsWith("+json")
                    }
                }.forEach { converter -> converter.defaultCharset = StandardCharsets.UTF_8 }
        }
    }

    companion object {
        const val API_BASE_PATH = "/api"

        /** The package of the controllers of Coffee Kitty. */
        const val CONTROLLER_PACKAGE = "de.seuhd.campuscoffee.api.controller"
    }
}
