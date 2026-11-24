package de.seuhd.campuscoffee.api.security

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
 * `X-Acting-User`. [securityFilterChain] requires authentication for the endpoint of every controller function with the
 * annotation [ActingUserRequired]. A request to such an endpoint without an acting user then gets its 401 before Spring
 * MVC reads and validates the request body. Every other endpoint is open to every client.
 *
 * The filter authenticates each request separately, so the API stores no sessions and sets no cookies. The
 * configuration turns off sessions, CSRF protection, the login form, HTTP Basic, and the logout endpoint.
 */
@Configuration
class SecurityConfig {
    /**
     * Returns the filter chain for every request. The endpoints of the controller functions with [ActingUserRequired]
     * require an acting user: creating a user, correcting a coffee count, and listing the corrections of a user.
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
     * Returns the HTTP method and the path pattern of every controller function with [ActingUserRequired], such as
     * `PUT` and `/api/users/{userId}/consumption`. The paths contain the prefix that `ApiWebConfig` adds.
     *
     * @throws IllegalStateException if no controller function has the annotation. Every endpoint would then be open, so
     *   the app does not start.
     */
    private fun actingUserEndpoints(handlerMapping: RequestMappingHandlerMapping): List<Pair<HttpMethod, String>> {
        val endpoints =
            handlerMapping.handlerMethods
                .filterValues { it.hasMethodAnnotation(ActingUserRequired::class.java) }
                .keys
                .flatMap { mapping ->
                    mapping.methodsCondition.methods.flatMap { method ->
                        mapping.patternValues.map { path -> method.asHttpMethod() to path }
                    }
                }
        check(endpoints.isNotEmpty()) { "No controller function has the annotation ActingUserRequired." }
        return endpoints
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
