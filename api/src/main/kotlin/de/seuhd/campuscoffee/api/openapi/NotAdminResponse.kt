package de.seuhd.campuscoffee.api.openapi

import io.swagger.v3.oas.annotations.responses.ApiResponse

/** Documents the 403 of an endpoint that only an admin may use. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(responseCode = "403", description = "The acting user is not an admin.")
annotation class NotAdminResponse
