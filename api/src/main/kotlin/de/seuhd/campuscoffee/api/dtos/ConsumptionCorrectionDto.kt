package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.CountCorrection
import io.swagger.v3.oas.annotations.media.Schema

/**
 * The request body of `PUT /api/users/{userId}/consumption`, with which an admin corrects the coffee count of a user to
 * [total] and explains the correction in [note] (FR-8). The coffee app of SE@UHD has a DTO with the same name and
 * fields, in which the note is optional. Neither property is nullable, so Jackson rejects a body without one of them
 * (ADR-008). The domain checks the values, and the `@Schema` annotations only document their limits (ADR-009). The
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
        description = "The note that explains the correction, which must not be blank or contain the NUL character.",
        maxLength = CountCorrection.MAX_NOTE_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val note: String
)
