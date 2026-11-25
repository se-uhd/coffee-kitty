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
 * The Spring Security configuration of Coffee Kitty. The coffee app of SE@UHD has a class with the same name.
 * [ActingUserAuthenticationFilter] runs before the controllers and resolves the acting user from the header
 * `X-Acting-User`. [securityFilterChain] requires authentication for the endpoint of every controller function of
 * Coffee Kitty, unless the function has the annotation [OpenEndpoint]. A request to such an endpoint without an acting
 * user gets its 401 before Spring MVC reads the request body. An endpoint is therefore open to every client only if a
 * developer marked its function, and a new endpoint without the annotation needs an acting user.
 *
 * The filter authenticates each request separately, so the API stores no sessions and sets no cookies. The
 * configuration turns off sessions, CSRF protection, the login form, HTTP Basic, and the logout endpoint.
 */
@Configuration
class SecurityConfig {
    /**
     * Returns the filter chain for every request. The endpoints of the controller functions without [OpenEndpoint]
     * require an acting user, such as creating a user, correcting a coffee count, and listing the corrections of a
     * user. A path that no controller of Coffee Kitty serves stays open, so that an unknown path gets the 404 of Spring
     * MVC and the dev profile can serve Swagger UI.
     *
     * @param http        the builder of the filter chain
     * @param userService the port with which [ActingUserAuthenticationFilter] looks up the acting user
     * @param entryPoint  the entry point that responds with 401 to a request without a known acting user
     * @param exceptionResolver the resolver of Spring MVC, which turns an exception of the lookup into the error
     *   response of `GlobalExceptionHandler`
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

    /**
     * Returns the HTTP method and the path pattern of every controller function of Coffee Kitty without
     * [OpenEndpoint], such as `PUT` and `/api/users/{userId}/consumption`. The paths contain the prefix that
     * `ApiWebConfig` adds. For a `GET` endpoint, the result also contains `HEAD`, because Spring MVC answers a `HEAD`
     * request with the controller function of the `GET` endpoint.
     *
     * @throws IllegalStateException if Spring MVC has no controller function of Coffee Kitty, or if such a function
     *   has no HTTP method. This function would then protect no endpoint or not every method, so the app does not
     *   start.
     */
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
     * Responds with 400 Bad Request and an `ErrorResponse` when the firewall of Spring Security rejects a request.
     * Coffee Kitty uses the default firewall, `StrictHttpFirewall`. It rejects, among others, a path with `;`, `//`,
     * `..`, or an encoded slash. Security rules and Spring MVC can read such a path differently, and a client could
     * then reach a controller that a rule protects. Coffee Kitty uses no matrix variables and no session ids in URLs
     * and needs none of these paths. As in [RequestRejectedExceptionHandler], the message does not quote the request,
     * and the log line contains only the HTTP method of the request and the exception type.
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
