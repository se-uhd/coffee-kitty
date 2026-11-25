package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.CountCorrection
import io.swagger.v3.oas.annotations.media.Schema

/**
 * The request body of `PUT /api/users/{userId}/consumption`, with which an admin corrects the coffee count of a user to
 * [total] and explains the correction in [note] (FR-8). The coffee app of SE@UHD has a DTO with the same name and the
 * fields `total` and `note`, in which the note is optional.
 * The class checks only the structure of the body. Neither property is nullable, so Jackson rejects a body without a
 * [total] or without a [note], and the request gets a 400 with the error code `BadRequest`. The domain checks the
 * values in `CoffeeConsumptionService.correctCount`. It rejects a [total] that is negative and a [note] that is blank,
 * is longer than [CountCorrection.MAX_NOTE_LENGTH] characters, or contains the NUL character. It throws a
 * `ValidationException` (400). The `@Schema` annotations only document these limits in the OpenAPI definition. The
 * [total] has no upper limit except the largest value of an `Int`, and Jackson rejects a higher number.
 */
data class ConsumptionCorrectionDto(
    @field:Schema(
        description = "The corrected coffee count.",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val total: Int,
    @field:Schema(
        description = "The reason for the correction, which must not be blank.",
        maxLength = CountCorrection.MAX_NOTE_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val note: String
)
