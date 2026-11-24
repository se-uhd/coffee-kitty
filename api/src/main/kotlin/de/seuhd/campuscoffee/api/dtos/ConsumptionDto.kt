package de.seuhd.campuscoffee.api.dtos

/**
 * Response body of the endpoints under `/api/users/{userId}/consumption` that read or change the coffee count, with
 * the [loginName] of the user and the coffee [count].
 */
data class ConsumptionDto(
    val loginName: String,
    val count: Int
)
