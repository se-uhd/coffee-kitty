package de.seuhd.campuscoffee.api.security

/**
 * Marks a controller function that needs an acting user, that is, a function that calls
 * [CurrentUserProvider.currentUser]. `SecurityConfig` reads the HTTP method and the path of every function with this
 * annotation from Spring MVC and requires authentication for these routes. A request without a known acting user then
 * gets its 401 before Spring MVC reads and validates the request body. No class repeats the path of the route as text.
 *
 * An architecture test fails if a controller function calls [CurrentUserProvider.currentUser] without this annotation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActingUserRequired
