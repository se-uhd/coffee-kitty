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
import tools.jackson.databind.json.JsonMapper

/**
 * The Spring Security configuration of Coffee Kitty, which the coffee app of SE@UHD also has under this name.
 * [ActingUserAuthenticationFilter] runs before the controllers and resolves the acting user from the header
 * `X-Acting-User`. A route whose controller calls [CurrentUserProvider.currentUser] needs a rule in
 * [securityFilterChain] that requires authentication. A request to such a route without an acting user then gets its
 * 401 before Spring MVC reads and validates the request body. Every other route is open to every client.
 *
 * The API keeps no sessions and sets no cookies. The filter authenticates each request on its own, so the
 * configuration turns off sessions, CSRF protection, the login form, HTTP Basic, and the logout endpoint.
 */
@Configuration
class SecurityConfig {
    /**
     * Returns the filter chain for every request. Only `POST /api/users` requires an acting user.
     *
     * @param http        the builder of the filter chain
     * @param userService the port with which [ActingUserAuthenticationFilter] looks up the acting user
     * @param entryPoint  the entry point that answers a request without a known acting user with 401
     * @param exceptionResolver the resolver of Spring MVC, which answers an unexpected exception of the filter with the
     *   error response of `GlobalExceptionHandler`
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        userService: UserService,
        entryPoint: JsonAuthenticationEntryPoint,
        @Qualifier("handlerExceptionResolver") exceptionResolver: HandlerExceptionResolver
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/users").authenticated()
                it.anyRequest().permitAll()
            }.exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .addFilterBefore(
                ActingUserAuthenticationFilter(userService, entryPoint, exceptionResolver),
                AnonymousAuthenticationFilter::class.java
            ).build()

    /**
     * Answers a request that the firewall of Spring Security rejects with 400 Bad Request and an `ErrorResponse`.
     * Spring Security keeps its default firewall, `StrictHttpFirewall`, which rejects, among others, a path with `;`,
     * `//`, `..`, or an encoded slash. Security rules and Spring MVC can read such a path differently, and a client
     * could then reach a controller that a rule protects. Coffee Kitty uses no matrix variables and no session ids in
     * URLs, so it needs none of these paths. The message does not quote the request, and the log names only the
     * exception type, as in [RequestRejectedExceptionHandler].
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
