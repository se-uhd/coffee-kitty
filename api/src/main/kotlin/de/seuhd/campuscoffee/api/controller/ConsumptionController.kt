package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.ConsumptionCorrectionDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.CountCorrectionDto
import de.seuhd.campuscoffee.api.mapper.ConsumptionDtoMapper
import de.seuhd.campuscoffee.api.mapper.CountCorrectionDtoMapper
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

/**
 * Controller under `/api/users/{userId}/consumption` that reads and corrects the coffee count of a user, adds and
 * undoes coffees, and lists the corrections. The
 * corrections are a list of [CountCorrectionDto]. Every other endpoint returns the login name and the coffee count of
 * the user as a [ConsumptionDto]. Only an admin may correct a coffee count and list the corrections. The header
 * `X-Acting-User` of these requests contains the login name of the acting user (see [CurrentUserProvider]).
 */
@Tag(
    name = "Consumption",
    description =
        "Reading a user's coffee count, adding a coffee, undoing a coffee, correcting a " +
            "coffee count (admin), and listing the corrections (admin)."
)
@RestController
@RequestMapping("/users/{userId}/consumption")
class ConsumptionController(
    private val coffeeConsumptionService: CoffeeConsumptionService,
    private val consumptionDtoMapper: ConsumptionDtoMapper,
    private val countCorrectionDtoMapper: CountCorrectionDtoMapper,
    private val currentUserProvider: CurrentUserProvider
) {
    /**
     * Returns the coffee count of the user with [userId].
     *
     * @param userId the id of the user
     */
    @Operation(summary = "Get a user's coffee count.")
    @GetMapping("")
    fun get(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<ConsumptionDto> =
        ResponseEntity.ok(consumptionDtoMapper.toDto(coffeeConsumptionService.getByUserId(userId)))

    /**
     * Adds the delta in [dto] to the coffee count of the user with [userId] and returns the new coffee count. The
     * domain accepts a delta of `+1`, which adds a coffee, and rejects any other integer with a `ValidationException`
     * (400).
     *
     * @param userId the id of the user
     * @param dto    the request body with the delta, which must be `+1`
     */
    @Operation(summary = "Add a coffee to a user's coffee count (a delta of +1).")
    @PostMapping("")
    fun change(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID,
        @ApiRequestBody(description = "The delta to apply, which must be +1.", required = true)
        @RequestBody
        @Valid dto: ConsumptionDeltaDto
    ): ResponseEntity<ConsumptionDto> {
        val updated = coffeeConsumptionService.applyDelta(userId, requireNotNull(dto.delta))
        return ResponseEntity.ok(consumptionDtoMapper.toDto(updated))
    }

    /**
     * Undoes the most recent coffee of the user with [userId] and returns the new coffee count. The domain throws a
     * `ConflictException` (409) when the user has no coffee to undo or when the grace period of the most recent coffee
     * has ended. The grace period is 5 minutes by default, and `campus-coffee.consumption.grace-period` sets it.
     *
     * @param userId the id of the user
     */
    @Operation(summary = "Undo the user's most recent coffee within the grace period after adding it.")
    @PostMapping("/undo")
    fun undo(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<ConsumptionDto> =
        ResponseEntity.ok(consumptionDtoMapper.toDto(coffeeConsumptionService.undo(userId)))

    /**
     * Corrects the coffee count of the user with [userId] to the total in [dto], stores the correction with the note in
     * [dto] (FR-8), and returns the corrected coffee count. The response is 401 Unauthorized without a known acting
     * user and 403 Forbidden if the acting user is not an admin. It is 400 Bad Request for a missing or negative total,
     * for a total above 1,000,000, and for a note that breaks a rule of [ConsumptionCorrectionDto]. The coffee app of
     * SE@UHD has an endpoint with the same HTTP method, path, and function name.
     *
     * @param userId the id of the user whose coffee count the admin corrects
     * @param dto    the request body with the corrected coffee count and the note with the reason for the correction
     */
    @Operation(summary = "Correct a user's coffee count and add a note with the reason (admin).")
    @Parameter(
        `in` = ParameterIn.HEADER,
        name = CurrentUserProvider.ACTING_USER_HEADER,
        description = "Login name of the acting user, who has to be an admin.",
        required = true
    )
    @PutMapping("")
    fun correctCount(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID,
        @ApiRequestBody(
            description = "The corrected coffee count, from 0 to 1,000,000, and a note with the reason.",
            required = true
        )
        @RequestBody
        @Valid dto: ConsumptionCorrectionDto
    ): ResponseEntity<ConsumptionDto> {
        val corrected =
            coffeeConsumptionService.correctCount(
                userId,
                requireNotNull(dto.total),
                dto.note,
                currentUserProvider.currentUser()
            )
        return ResponseEntity.ok(consumptionDtoMapper.toDto(corrected))
    }

    /**
     * Returns the corrections of the coffee count of the user with [userId], newest first. The response is 401
     * Unauthorized without a known acting user and 403 Forbidden if the acting user is not an admin.
     *
     * @param userId the id of the user whose corrections to return
     */
    @Operation(summary = "List the corrections of a user's coffee count, newest first (admin).")
    @Parameter(
        `in` = ParameterIn.HEADER,
        name = CurrentUserProvider.ACTING_USER_HEADER,
        description = "Login name of the acting user, who has to be an admin.",
        required = true
    )
    @GetMapping("/corrections")
    fun getCorrections(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<List<CountCorrectionDto>> {
        val corrections = coffeeConsumptionService.getCorrections(userId, currentUserProvider.currentUser())
        return ResponseEntity.ok(corrections.map { countCorrectionDtoMapper.toDto(it) })
    }
}
