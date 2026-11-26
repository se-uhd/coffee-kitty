package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.ConsumptionCorrectionDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.dtos.CountCorrectionDto
import de.seuhd.campuscoffee.api.dtos.UndoStatusDto
import de.seuhd.campuscoffee.api.mapper.ConsumptionDtoMapper
import de.seuhd.campuscoffee.api.mapper.CountCorrectionDtoMapper
import de.seuhd.campuscoffee.api.openapi.NotAdminResponse
import de.seuhd.campuscoffee.api.openapi.UserNotFoundResponse
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.api.security.OpenEndpoint
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

/**
 * The controller under `/api/users/{userId}/consumption` that reads and corrects the coffee count of a user, adds and
 * undoes coffees, reads the undo status, and lists the corrections. The undo status is an [UndoStatusDto], and the
 * corrections are a list of [CountCorrectionDto]. Every other endpoint returns the login name and the coffee count of
 * the user as a [ConsumptionDto]. Only an admin may correct a coffee count and list the corrections. The header
 * `X-Acting-User` of these requests contains the login name of the acting user (see [CurrentUserProvider]).
 */
@Tag(
    name = "Consumption",
    description =
        "Reading and correcting a coffee count, adding and undoing coffees, and reading the undo status and the " +
            "corrections."
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
    @ResponseStatus(HttpStatus.OK)
    @UserNotFoundResponse
    @OpenEndpoint
    @GetMapping("")
    fun get(
        @Parameter(description = "The id of the user.", required = true)
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
    @ResponseStatus(HttpStatus.OK)
    @UserNotFoundResponse
    @ApiResponse(responseCode = "409", description = "The coffee count is at its maximum of 2,147,483,647.")
    @OpenEndpoint
    @PostMapping("")
    fun applyDelta(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable userId: UUID,
        @ApiRequestBody(description = "The delta to apply, which must be +1.", required = true)
        @RequestBody
        dto: ConsumptionDeltaDto
    ): ResponseEntity<ConsumptionDto> {
        val updated = coffeeConsumptionService.applyDelta(userId, dto.delta)
        return ResponseEntity.ok(consumptionDtoMapper.toDto(updated))
    }

    /**
     * Undoes the most recent coffee of the user with [userId] and returns the new coffee count. The domain throws a
     * `ConflictException` (409) when the user has no coffee to undo or when the grace period of the most recent coffee
     * has ended. The grace period is 5 minutes by default, and `campus-coffee.consumption.grace-period` sets it.
     *
     * @param userId the id of the user
     */
    @Operation(summary = "Undo a user's most recent coffee within its grace period.")
    @ResponseStatus(HttpStatus.OK)
    @UserNotFoundResponse
    @ApiResponse(
        responseCode = "409",
        description = "The user has no coffee, or the grace period of the most recent coffee has passed."
    )
    @OpenEndpoint
    @PostMapping("/undo")
    fun undo(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<ConsumptionDto> =
        ResponseEntity.ok(consumptionDtoMapper.toDto(coffeeConsumptionService.undo(userId)))

    /**
     * Returns the undo status of the user with [userId] (FR-9). The undo status shows whether the user can undo their
     * most recent coffee at the time of the request and, if so, until when. The response is also 200 OK when the user
     * cannot undo a coffee.
     *
     * @param userId the id of the user
     */
    @Operation(summary = "Get the undo status of a user.")
    @ResponseStatus(HttpStatus.OK)
    @UserNotFoundResponse
    @OpenEndpoint
    @GetMapping("/undo")
    fun undoStatus(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<UndoStatusDto> {
        val undoableUntil = coffeeConsumptionService.undoableUntil(userId)
        return ResponseEntity.ok(UndoStatusDto(undoable = undoableUntil != null, undoableUntil = undoableUntil))
    }

    /**
     * Corrects the coffee count of the user with [userId] to the total in [dto] (FR-8) and returns the corrected coffee
     * count. The coffee app of SE@UHD has an endpoint with the same HTTP method, path, and name.
     *
     * @param userId the id of the user whose coffee count the admin corrects
     * @param dto    the request body with the corrected coffee count and the note with the reason for the correction
     */
    @Operation(summary = "Correct a user's coffee count with a note (admin).")
    @ResponseStatus(HttpStatus.OK)
    @NotAdminResponse
    @UserNotFoundResponse
    @Parameter(
        `in` = ParameterIn.HEADER,
        name = CurrentUserProvider.ACTING_USER_HEADER,
        description = "The login name of the acting user, who has to be an admin.",
        required = true
    )
    @PutMapping("")
    fun correctCount(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable userId: UUID,
        @ApiRequestBody(
            description = "The corrected coffee count, which must not be negative, and a note with the reason.",
            required = true
        )
        @RequestBody
        dto: ConsumptionCorrectionDto
    ): ResponseEntity<ConsumptionDto> {
        val corrected =
            coffeeConsumptionService.correctCount(
                userId,
                dto.total,
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
    @Operation(summary = "List the corrections of a user, newest first (admin).")
    @ResponseStatus(HttpStatus.OK)
    @NotAdminResponse
    @UserNotFoundResponse
    @Parameter(
        `in` = ParameterIn.HEADER,
        name = CurrentUserProvider.ACTING_USER_HEADER,
        description = "The login name of the acting user, who has to be an admin.",
        required = true
    )
    @GetMapping("/corrections")
    fun getCorrections(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<List<CountCorrectionDto>> {
        val corrections = coffeeConsumptionService.getCorrections(userId, currentUserProvider.currentUser())
        return ResponseEntity.ok(corrections.map { countCorrectionDtoMapper.toDto(it) })
    }
}
