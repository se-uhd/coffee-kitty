package de.seuhd.campuscoffee.api.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * One correction in the response of `GET /api/users/{userId}/consumption/corrections`: The admin with
 * [adminLoginName] set the coffee count from [countBefore] to [countAfter] and explained the correction in [note]. The
 * class does not implement [Dto], because no endpoint reads or changes a single correction by id.
 */
data class CountCorrectionDto(
    @field:Schema(description = "The coffee count before the correction.")
    val countBefore: Int,
    @field:Schema(description = "The coffee count after the correction.")
    val countAfter: Int,
    @field:Schema(description = "The note with the reason for the correction.")
    val note: String,
    @field:Schema(description = "The login name of the admin who corrected the coffee count.")
    val adminLoginName: String,
    @field:Schema(description = "When the admin corrected the coffee count, in UTC.")
    val createdAt: LocalDateTime
)
