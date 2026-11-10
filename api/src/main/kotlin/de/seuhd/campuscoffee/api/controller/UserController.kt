package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.mapper.UserDtoMapper
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.domain.ports.api.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.util.UUID
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

/**
 * Controller under `/api/users` that lists all users, reads a user by id, and creates a user. For each new user, the
 * domain creates a consumption with a coffee count of zero, which [ConsumptionController] serves.
 */
@Tag(name = "Users", description = "Listing, reading, and creating users.")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper
) {
    /**
     * Returns every user, ordered by login name. The endpoint does not split the list into pages, because a coffee
     * group has few users.
     */
    @Operation(summary = "Get all users.")
    @GetMapping("")
    fun getAll(): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(userService.getAll().map { userDtoMapper.fromDomain(it) })

    /**
     * Returns the user with [id].
     *
     * @param id the id of the user
     */
    @Operation(summary = "Get a user by ID.")
    @GetMapping("/{id}")
    fun getById(
        @Parameter(description = "Unique identifier of the user to retrieve.", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<UserDto> = ResponseEntity.ok(userDtoMapper.fromDomain(userService.getById(id)))

    /**
     * Creates a user and returns 201 Created with the URL of the new user in the `Location` header. In the same
     * transaction, the domain creates the consumption of the user with a coffee count of zero.
     *
     * `ResponseEntity.created` sets the status of the response. springdoc reads `@ResponseStatus` and documents the
     * status 201 instead of 200 in the OpenAPI definition.
     *
     * @param dto the user to create, without an id
     * @throws ValidationException if the body contains an id
     */
    @Operation(summary = "Create a user.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("")
    fun create(
        @ApiRequestBody(description = "The user to create, without an ID.", required = true)
        @RequestBody
        @Valid dto: UserDto
    ): ResponseEntity<UserDto> {
        if (dto.id != null) {
            throw ValidationException("ID must not be set when creating a user.")
        }
        val created = userService.create(userDtoMapper.toDomain(dto))
        return ResponseEntity.created(locationOf(created.persistedId)).body(userDtoMapper.fromDomain(created))
    }

    /**
     * Returns the URL for the `Location` header of a new user: the URL of the current request without its query
     * string, followed by `/` and [id]. `UserSystemTests` checks that the query string is left out.
     *
     * @param id the id of the new user
     */
    private fun locationOf(id: UUID): URI =
        ServletUriComponentsBuilder
            .fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(id)
            .toUri()
}
