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
 * Controller for the users. It lists all users, reads one by id, and adds a user. The domain creates the coffee
 * count of a new user at zero, and [ConsumptionController] serves that count.
 */
@Tag(name = "Users", description = "Listing, reading, and adding users.")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper
) {
    /**
     * Returns every user, ordered by login name. The list is not paged, because a coffee group is small enough to
     * list at once.
     */
    @Operation(summary = "Get all users.")
    @GetMapping("")
    fun getAll(): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(userService.getAll().map { userDtoMapper.fromDomain(it) })

    /**
     * Retrieves one user by id.
     *
     * @param id the id of the user to retrieve
     */
    @Operation(summary = "Get a user by ID.")
    @GetMapping("/{id}")
    fun getById(
        @Parameter(description = "Unique identifier of the user to retrieve.", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<UserDto> = ResponseEntity.ok(userDtoMapper.fromDomain(userService.getById(id)))

    /**
     * Adds a user and answers 201 Created with the new user's URL in the `Location` header. The domain creates the
     * user's coffee count at zero in the same transaction.
     *
     * @param dto the user to add, without an id
     * @throws ValidationException if the body carries an id
     */
    @Operation(summary = "Add a user.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("")
    fun create(
        @ApiRequestBody(description = "The user to add, without an id.", required = true)
        @RequestBody
        @Valid dto: UserDto
    ): ResponseEntity<UserDto> {
        if (dto.id != null) {
            throw ValidationException("ID must not be set when creating a new resource.")
        }
        val created = userService.create(userDtoMapper.toDomain(dto))
        return ResponseEntity.created(locationOf(created.persistedId)).body(userDtoMapper.fromDomain(created))
    }

    /**
     * The location of a newly created user for the 201 response: the collection URL of the current request
     * followed by the new id.
     *
     * @param id the new user's id
     */
    private fun locationOf(id: UUID): URI =
        ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(id)
            .toUri()
}
