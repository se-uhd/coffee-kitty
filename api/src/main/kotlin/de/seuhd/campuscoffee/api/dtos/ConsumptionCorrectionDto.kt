package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.CountCorrection
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Request body of `PUT /api/users/{userId}/consumption`, with which an admin corrects the coffee count of a user to
 * [total] and explains the correction in [note] (FR-8). The coffee app of SE@UHD has a DTO with the same name and the
 * fields `total` and `note`, in which the note is optional.
 *
 * Bean Validation rejects a [total] that is missing, negative, or above [CountCorrection.MAX_TOTAL]. It also rejects a
 * [note] that is missing, blank, or longer than [CountCorrection.MAX_NOTE_LENGTH] characters. The request then gets a
 * 400 with the error code `BadRequest`. The domain checks the total and the note again and also rejects a note with the
 * NUL character. It throws a `ValidationException` (400).
 */
data class ConsumptionCorrectionDto(
    @field:NotNull(message = "is required")
    @field:Min(value = 0, message = "must not be negative")
    @field:Max(value = CountCorrection.MAX_TOTAL.toLong(), message = "must not exceed {value}")
    val total: Int?,
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = CountCorrection.MAX_NOTE_LENGTH, message = "must have at most {max} characters")
    val note: String?
)
