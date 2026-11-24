package de.seuhd.campuscoffee.api.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Response body of `GET /api/users/{userId}/consumption/undo`, the undo status of a user (FR-9). [undoable] is true
 * when the user can undo their most recent coffee at the time of the request. [undoableUntil] is the end of the grace
 * period of that coffee in UTC, and it is null when [undoable] is false. The coffee app of SE@UHD returns fields with
 * the same names in its summaries.
 */
data class UndoStatusDto(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val undoable: Boolean,
    @field:Schema(
        description = "The end of the grace period in UTC, or null if the user cannot undo a coffee.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    val undoableUntil: LocalDateTime?
)
