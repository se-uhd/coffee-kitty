package de.seuhd.campuscoffee.api.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * One correction in the response of `GET /api/users/{userId}/consumption/corrections`: The admin with
 * [adminLoginName] set the coffee count from [countBefore] to [countAfter] and explained the correction in [note]. The
 * class has no id, because no endpoint reads a single correction by id.
 */
data class CountCorrectionDto(
    @field:Schema(description = "The coffee count before the correction.", requiredMode = Schema.RequiredMode.REQUIRED)
    val countBefore: Int,
    @field:Schema(description = "The coffee count after the correction.", requiredMode = Schema.RequiredMode.REQUIRED)
    val countAfter: Int,
    @field:Schema(
        description = "The note with the reason for the correction.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val note: String,
    @field:Schema(
        description = "The login name of the admin who corrected the coffee count.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val adminLoginName: String,
    @field:Schema(
        description = "When the admin corrected the coffee count, in UTC.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val createdAt: LocalDateTime
)
