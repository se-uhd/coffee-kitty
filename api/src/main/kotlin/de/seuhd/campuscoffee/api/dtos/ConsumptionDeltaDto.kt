package de.seuhd.campuscoffee.api.dtos

import io.swagger.v3.oas.annotations.media.Schema

/**
 * The request body of `POST /api/users/{userId}/consumption`, which adds a coffee with a [delta] of `+1`. Jackson
 * rejects a [delta] with a fraction, such as 1.9, or a JSON string, such as "1", because `application.yaml` sets
 * `spring.jackson.deserialization.accept-float-as-int` and `spring.jackson.mapper.allow-coercion-of-scalars` to false.
 * Such a request gets a 400 with the error code `BadRequest`. The domain rejects any integer other than `+1` with a
 * `ValidationException` (ADR-009).
 */
data class ConsumptionDeltaDto(
    @field:Schema(
        description = "The change of the coffee count, which must be +1.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val delta: Int
)
