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
 * Spring MVC configuration of the REST API. It adds the prefix [API_BASE_PATH] to the path of every controller in the
 * package `api.controller`, and the controllers leave the prefix out of their `@RequestMapping`.
 * [configureMessageConverters] restricts the request and response bodies to JSON, with responses in UTF-8.
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
     * Removes every XML and every YAML message converter and adds the charset UTF-8 to the media types of every JSON
     * message converter.
     *
     * Without an XML message converter, a client cannot get an XML response by sending `Accept: application/xml`.
     * Spring registers a JAXB message converter for XML, because Hibernate puts JAXB on the runtime classpath. This
     * method removes the JAXB message converter and any XML message converter that a later dependency adds.
     *
     * Spring also registers a YAML message converter, because springdoc puts the YAML module of Jackson on the
     * runtime classpath. With this converter, a client would get a YAML response by sending
     * `Accept: application/yaml`, and a request with a YAML body would get a 500. Without the converter, a request with
     * a YAML body gets a 415 Unsupported Media Type, and a request with that `Accept` header gets a 406 Not Acceptable.
     * `ErrorResponseSystemTests` checks both statuses.
     *
     * Spring sends JSON as `application/json` without a `charset` parameter, because RFC 8259 requires UTF-8 for JSON
     * and does not define a charset parameter. Without the charset, a client that assumes ISO-8859-1
     * decodes `Müller` as `MÃ¼ller`. With the charset on the JSON message converters, every JSON response has the
     * content type `application/json;charset=UTF-8`, which `UserSystemTests` checks.
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
