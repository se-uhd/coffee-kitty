package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * Response body for a user. Every property is non-null, because the server returns only stored users, which have an id
 * and timestamps. The OpenAPI definition therefore marks every field as required. [CreateUserDto] is the request body
 * for a new user. The timestamps are in UTC without an offset.
 */
data class UserDto(
    val id: UUID,
    @field:Schema(description = "When the user was created, in UTC.")
    val createdAt: LocalDateTime,
    @field:Schema(description = "When the user was last changed, in UTC.")
    val updatedAt: LocalDateTime,
    val loginName: String,
    val emailAddress: String,
    val firstName: String,
    val lastName: String,
    val role: Role
)
