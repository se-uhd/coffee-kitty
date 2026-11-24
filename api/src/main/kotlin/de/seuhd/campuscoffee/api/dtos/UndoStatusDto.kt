package de.seuhd.campuscoffee.api.dtos

import java.time.LocalDateTime

/**
 * Response body of `GET /api/users/{userId}/consumption/undo`, the undo status of a user (FR-9). [undoable] is true
 * when the user can undo their most recent coffee at the time of the request. [undoableUntil] is the end of the grace
 * period of that coffee in UTC, and it is null when [undoable] is false. The coffee app of SE@UHD returns fields with
 * the same names in its summaries. The class does not implement [Dto], because it has no id.
 */
data class UndoStatusDto(
    val undoable: Boolean,
    val undoableUntil: LocalDateTime?
)
