package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.CountCorrection
import io.swagger.v3.oas.annotations.media.Schema

/**
 * The request body of `PUT /api/users/{userId}/consumption`, with which an admin corrects the coffee count of a user to
 * [total], can explain the correction in [note], and can send the [expectedCount] that the admin saw (FR-8). The coffee
 * app of SE@UHD has a DTO with the same name and fields. Jackson rejects a body without [total] (ADR-008). The domain
 * checks the values, and the `@Schema` annotations only document their limits (ADR-009).
 */
data class ConsumptionCorrectionDto(
    @field:Schema(
        description = "The corrected coffee count.",
        minimum = "0",
        maximum = "${CountCorrection.MAX_CORRECTED_COUNT}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val total: Int,
    @field:Schema(
        description = "The reason for the correction without the NUL character. A blank note counts as no note.",
        maxLength = CountCorrection.MAX_NOTE_LENGTH
    )
    val note: String? = null,
    @field:Schema(
        description = "The coffee count that the admin saw. If the count differs from it, the response is 409."
    )
    val expectedCount: Int? = null
)
