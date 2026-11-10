package de.seuhd.campuscoffee.api.dtos

import jakarta.validation.constraints.NotNull

/**
 * Request body of `POST /api/users/{userId}/consumption`, which adds a coffee with a [delta] of `+1`.
 *
 * Jackson rejects a [delta] with a fraction, such as 1.9, because `application.yaml` sets
 * `spring.jackson.deserialization.accept-float-as-int` to false. Bean Validation rejects a body without a [delta].
 * Both rejections return a 400 with the error code `BadRequest`. The domain rejects any integer other than `+1` with
 * a `ValidationException` (400).
 */
data class ConsumptionDeltaDto(
    @field:NotNull
    val delta: Int?
)
