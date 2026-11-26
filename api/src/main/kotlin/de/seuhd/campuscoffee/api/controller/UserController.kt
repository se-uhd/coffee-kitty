package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.CreateUserDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.api.mapper.UserDtoMapper
import de.seuhd.campuscoffee.api.openapi.NotAdminResponse
import de.seuhd.campuscoffee.api.openapi.UserNotFoundResponse
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.api.security.OpenEndpoint
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.domain.ports.api.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
 * The controller under `/api/users` that lists all users, reads a user by id, and creates a user. For each new user,
 * the domain creates a consumption with a coffee count of zero. The endpoints of [ConsumptionController] read and
 * change this consumption.
 */
@Tag(name = "Users", description = "Listing, reading, and creating users.")
@RestController
// ADR-024 Spring refuses an Accept header without JSON before the function runs
@RequestMapping("/users", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserController(
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper,
    private val currentUserProvider: CurrentUserProvider
) {
    /**
     * Returns every user, ordered by login name. The endpoint does not split the list into pages, because a coffee
     * group has few users.
     */
    @Operation(summary = "Get all users.")
    @OpenEndpoint
    @GetMapping("")
    fun getAll(): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(userService.getAll().map { userDtoMapper.toDto(it) })

    /**
     * Returns the user with [id].
     *
     * @param id the id of the user
     */
    @Operation(summary = "Get a user by id.")
    @ResponseStatus(HttpStatus.OK)
    @UserNotFoundResponse
    @OpenEndpoint
    @GetMapping("/{id}")
    fun getById(
        @Parameter(description = "The id of the user.", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<UserDto> = ResponseEntity.ok(userDtoMapper.toDto(userService.getById(id)))

    /**
     * Creates a user and the user's consumption and returns 201 Created with the URL of the new user in the `Location`
     * header, or 403 Forbidden if the acting user is not an admin. `ResponseEntity.created` sets the status, and
     * `@ResponseStatus` makes springdoc document 201 instead of 200 in the OpenAPI definition.
     *
     * @param dto the fields of the user to create
     */
    @Operation(summary = "Create a user (admin).")
    @NotAdminResponse
    @ApiResponse(responseCode = "409", description = "Another user has the login name or the email address.")
    @Parameter(
        `in` = ParameterIn.HEADER,
        name = CurrentUserProvider.ACTING_USER_HEADER,
        description = "The login name of the acting user, who has to be an admin.",
        required = true
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("")
    fun create(
        @ApiRequestBody(description = "The fields of the user to create.", required = true)
        @RequestBody
        dto: CreateUserDto
    ): ResponseEntity<UserDto> {
        val created = userService.create(userDtoMapper.toDomain(dto), currentUserProvider.currentUser())
        return ResponseEntity.created(locationOf(created.persistedId)).body(userDtoMapper.toDto(created))
    }

    private fun locationOf(id: UUID): URI =
        ServletUriComponentsBuilder
            .fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(id)
            .toUri()
}
