package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.api.configuration.ApiWebConfig
import de.seuhd.campuscoffee.domain.ports.api.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.firewall.RequestRejectedHandler
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.json.JsonMapper

/**
 * The Spring Security configuration of Coffee Kitty. [securityFilterChain] requires an acting user for the endpoint of
 * every controller function without the annotation [OpenEndpoint] (ADR-011), so that a request without an acting user
 * gets its 401 before Spring MVC reads the request body. [ActingUserAuthenticationFilter] authenticates each request
 * separately, so the configuration turns off sessions, CSRF protection, the login form, HTTP Basic, and the logout
 * endpoint. The coffee app of SE@UHD has a class with the same name.
 */
@Configuration
class SecurityConfig {
    /**
     * Returns the filter chain for every request. A path that no controller of Coffee Kitty serves stays open, so that
     * an unknown path gets the 404 of Spring MVC and the dev profile can serve Swagger UI (ADR-011).
     *
     * @param http        the builder of the filter chain
     * @param userService the port with which [ActingUserAuthenticationFilter] looks up the acting user
     * @param entryPoint  the entry point that responds with 401 to a request without a known acting user
     * @param exceptionResolver the resolver that turns an exception of the lookup into an `ErrorResponse`
     * @param handlerMapping the mapping of Spring MVC from endpoints to controller functions
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        userService: UserService,
        entryPoint: JsonAuthenticationEntryPoint,
        @Qualifier("handlerExceptionResolver") exceptionResolver: HandlerExceptionResolver,
        @Qualifier("requestMappingHandlerMapping") handlerMapping: RequestMappingHandlerMapping
    ): SecurityFilterChain {
        val endpoints = actingUserEndpoints(handlerMapping)
        return http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                endpoints.forEach { (method, path) -> requests.requestMatchers(method, path).authenticated() }
                requests.anyRequest().permitAll()
            }.exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .addFilterBefore(
                ActingUserAuthenticationFilter(userService, entryPoint, exceptionResolver),
                AnonymousAuthenticationFilter::class.java
            ).build()
    }

    // ADR-011
    private fun actingUserEndpoints(handlerMapping: RequestMappingHandlerMapping): List<Pair<HttpMethod, String>> {
        val controllerFunctions =
            handlerMapping.handlerMethods.filterValues {
                it.beanType.packageName.startsWith(ApiWebConfig.CONTROLLER_PACKAGE)
            }
        check(controllerFunctions.isNotEmpty()) {
            "Spring MVC has no controller function in the package ${ApiWebConfig.CONTROLLER_PACKAGE}."
        }
        return controllerFunctions
            .filterValues { !it.hasMethodAnnotation(OpenEndpoint::class.java) }
            .flatMap { (mapping, function) ->
                val methods = mapping.methodsCondition.methods.map { it.asHttpMethod() }
                check(methods.isNotEmpty()) { "The controller function ${function.method.name} has no HTTP method." }
                val withHead = if (HttpMethod.GET in methods) methods + HttpMethod.HEAD else methods
                withHead.flatMap { method -> mapping.patternValues.map { path -> method to path } }
            }
    }

    /**
     * Responds with 400 Bad Request and an `ErrorResponse` when the firewall of Spring Security rejects a request, such
     * as a path with `;`, `//`, `..`, or an encoded slash, which security rules and Spring MVC can read differently. As
     * in [RequestRejectedExceptionHandler], the message does not quote the request.
     *
     * @param jsonMapper the JSON mapper of Spring MVC
     */
    @Bean
    fun requestRejectedHandler(jsonMapper: JsonMapper): RequestRejectedHandler =
        RequestRejectedHandler { request, response, exception ->
            log.info { "Rejected request on ${request.method}: ${exception::class.simpleName}" }
            response.writeErrorResponse(
                jsonMapper,
                HttpStatus.BAD_REQUEST,
                UNREADABLE_REQUEST_MESSAGE,
                request.requestURI
            )
        }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
