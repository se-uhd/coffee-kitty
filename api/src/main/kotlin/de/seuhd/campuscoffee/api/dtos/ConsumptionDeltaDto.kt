package de.seuhd.campuscoffee.api.dtos

import jakarta.validation.constraints.NotNull

/**
 * Request body for adding a coffee (`POST /api/users/{userId}/consumption`): a [delta] of `+1`. The DTO only
 * requires the field, and Bean Validation rejects a missing [delta] with 400. The domain checks the value and
 * rejects anything other than `+1` with a `ValidationException` (400).
 */
data class ConsumptionDeltaDto(
    @field:NotNull
    val delta: Int?
)
