package de.seuhd.campuscoffee.api.dtos

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response body of the endpoints under `/api/users/{userId}/consumption` that read or change the coffee count, with
 * the [loginName] of the user and the coffee [count].
 */
data class ConsumptionDto(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val loginName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val count: Int
)
