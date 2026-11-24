package de.seuhd.campuscoffee.api.dtos

import jakarta.validation.constraints.NotNull

/**
 * The request body of `POST /api/users/{userId}/consumption`, which adds a coffee with a [delta] of `+1`.
 *
 * Jackson rejects a [delta] with a fraction, such as 1.9, because `application.yaml` sets
 * `spring.jackson.deserialization.accept-float-as-int` to false. It also rejects a [delta] that is a JSON string, such
 * as "1", because `application.yaml` sets `spring.jackson.mapper.allow-coercion-of-scalars` to false. Bean Validation
 * rejects a body without a [delta]. In these cases, the request gets a 400 with the error code `BadRequest`. Like every
 * request DTO, the class checks only the structure of the body. The domain checks the value and rejects any integer
 * other than `+1` with a `ValidationException` (400).
 */
data class ConsumptionDeltaDto(
    @field:NotNull
    val delta: Int?
)
