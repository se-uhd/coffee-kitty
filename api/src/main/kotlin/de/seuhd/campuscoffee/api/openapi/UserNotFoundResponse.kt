package de.seuhd.campuscoffee.api.openapi

import io.swagger.v3.oas.annotations.responses.ApiResponse

/** Documents the 404 of an endpoint that looks up the user with the id in its path. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(responseCode = "404", description = "No user has the id.")
annotation class UserNotFoundResponse
