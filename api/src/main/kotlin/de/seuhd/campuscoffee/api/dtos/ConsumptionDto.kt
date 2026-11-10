package de.seuhd.campuscoffee.api.dtos

/**
 * Response body of both endpoints under `/api/users/{userId}/consumption`, with the [loginName] of the user and the
 * coffee [count]. The class does not implement [Dto], because it has no id.
 */
data class ConsumptionDto(
    val loginName: String,
    val count: Int
)
