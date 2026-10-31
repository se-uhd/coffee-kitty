package de.seuhd.campuscoffee.api.dtos

import jakarta.validation.constraints.NotNull

/**
 * Request body for adding a coffee (`POST /api/users/{userId}/consumption`): a [delta] of `+1`. Jackson rejects a
 * [delta] with a fraction (`spring.jackson.deserialization.accept-float-as-int: false` in `application.yaml`), and
 * Bean Validation rejects a missing [delta], both with 400 `BadRequest`. The domain rejects any other integer than `+1`
 * with a `ValidationException` (400).
 */
data class ConsumptionDeltaDto(
    @field:NotNull
    val delta: Int?
)
