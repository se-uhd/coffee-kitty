package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.api.mapper.ConsumptionDtoMapper
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

/**
 * Controller for a user's coffee count under `/users/{userId}/consumption`. It reads the count and adds a coffee to
 * it, and both operations return the user's login name and coffee count as a [ConsumptionDto].
 */
@Tag(name = "Consumption", description = "Reading a user's coffee count and adding a coffee.")
@RestController
@RequestMapping("/users/{userId}/consumption")
class ConsumptionController(
    private val coffeeConsumptionService: CoffeeConsumptionService,
    private val consumptionDtoMapper: ConsumptionDtoMapper
) {
    /**
     * Returns the coffee count of the user with [userId].
     *
     * @param userId the id of the user whose coffee count to read
     */
    @Operation(summary = "Get a user's coffee count.")
    @GetMapping("")
    fun get(
        @Parameter(description = "Unique identifier of the user.", required = true)
        @PathVariable userId: UUID
    ): ResponseEntity<ConsumptionDto> =
        ResponseEntity.ok(consumptionDtoMapper.toDto(coffeeConsumptionService.getByUserId(userId)))

    /**
     * Applies the delta in [dto] to the coffee count of the user with [userId] and returns the new count. The domain
     * accepts only a delta of `+1`, which adds a coffee, and rejects any other value with a `ValidationException`
     * (400).
     *
     * @param userId the id of the user whose coffee count to change
     * @param dto    the delta to apply, which must be `+1`
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
}
