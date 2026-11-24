package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * Response body for a user. Every property is non-null, because the server returns only stored users, which have an id
 * and timestamps. springdoc does not derive from a non-null Kotlin type that a field is always present, so each
 * property states it with `requiredMode`, and the OpenAPI definition marks every field as required. [CreateUserDto] is
 * the request body for a new user. The timestamps are in UTC without an offset.
 */
data class UserDto(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val id: UUID,
    @field:Schema(description = "When the user was created, in UTC.", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: LocalDateTime,
    @field:Schema(description = "When the user was last changed, in UTC.", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: LocalDateTime,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val loginName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val emailAddress: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val firstName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val lastName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val role: Role
)
