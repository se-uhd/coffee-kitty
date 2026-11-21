package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * Request and response body for a user. The properties are nullable, because Bean Validation can reject a missing
 * field only after Jackson has created the DTO from the request body. Validation runs before the controller maps the
 * DTO to a [User].
 *
 * The OpenAPI definition marks [id], [createdAt], and [updatedAt] as read-only, because the server assigns them.
 * `OpenApiSystemTests` checks that these three properties are read-only. The timestamps are in UTC without an offset.
 *
 * In the error response, the message of a failed field constraint follows the field name, as in "loginName must have 1
 * to 255 characters".
 *
 * [role] is optional in a request. [UserDtoMapper] sets a missing role to `USER`.
 *
 * Jackson rejects a JSON number or boolean for a `String` property, such as a [loginName] of 12345, because
 * `JsonConfig` turns off the conversion of such a value to its text. Jackson also rejects a JSON number for [role],
 * such as 1, because `application.yaml` sets `spring.jackson.datatype.enum.fail-on-numbers-for-enums` to true. These
 * rejections return a 400 with the error code `BadRequest`.
 */
data class UserDto(
    @field:Schema(accessMode = Schema.AccessMode.READ_ONLY)
    override val id: UUID? = null,
    @field:Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "When the user was created, in UTC.")
    val createdAt: LocalDateTime? = null,
    @field:Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "When the user was last changed, in UTC.")
    val updatedAt: LocalDateTime? = null,
    @field:NotNull
    @field:Size(min = 1, max = User.MAX_LOGIN_NAME_LENGTH, message = "must have {min} to {max} characters")
    @field:Pattern(
        regexp = User.LOGIN_NAME_PATTERN,
        message = "must contain only ASCII letters, digits, and underscores"
    )
    val loginName: String?,
    @field:NotNull
    @field:Email
    // @Email accepts an empty string and an address longer than the column `users.email_address` (254 characters).
    // @Size rejects both with a field error before the domain checks the address.
    @field:Size(
        min = User.MIN_EMAIL_LENGTH,
        max = User.MAX_EMAIL_LENGTH,
        message = "must have {min} to {max} characters"
    )
    val emailAddress: String?,
    @field:NotBlank
    @field:Size(max = User.MAX_NAME_LENGTH, message = "must have at most {max} characters")
    val firstName: String?,
    @field:NotBlank
    @field:Size(max = User.MAX_NAME_LENGTH, message = "must have at most {max} characters")
    val lastName: String?,
    @field:Schema(description = "The role of the user. A request without a role creates a USER.")
    val role: Role? = null
) : Dto<UUID>
