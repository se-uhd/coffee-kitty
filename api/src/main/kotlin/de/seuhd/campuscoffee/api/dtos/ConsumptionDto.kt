package de.seuhd.campuscoffee.api.dtos

/**
 * Response DTO for a user's coffee count: the user's [loginName] and their running [count] of coffees. Both
 * endpoints under `/api/users/{userId}/consumption` return it. It does not implement [Dto], because it carries no
 * entity id.
 */
data class ConsumptionDto(
    val loginName: String,
    val count: Int
)
