package de.seuhd.campuscoffee.api.security

/**
 * Marks a controller function with an endpoint that every client may use without an acting user, such as the endpoint
 * that reads a coffee count. Every controller function without this annotation needs an acting user: `SecurityConfig`
 * requires authentication for its endpoint, and a request without a known acting user gets its 401 before Spring MVC
 * reads the request body. A new endpoint therefore needs an acting user until a developer marks it as open.
 *
 * An architecture test fails if a controller function with this annotation calls [CurrentUserProvider.currentUser].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenEndpoint
