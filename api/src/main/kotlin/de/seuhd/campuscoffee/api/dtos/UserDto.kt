package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * DTO for a user. Properties are nullable, so a request body that omits a field deserializes and is then rejected
 * by Bean Validation. The controller validates the DTO before it is mapped to a [User]. The server assigns [id],
 * [createdAt], and [updatedAt].
 */
data class UserDto(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @field:NotNull
    @field:Size(
        min = 1,
        max = User.MAX_LOGIN_NAME_LENGTH,
        message = "Login name must be between {min} and {max} characters long."
    )
    @field:Pattern(
        regexp = User.LOGIN_NAME_PATTERN,
        message = "Login name can only contain word characters: [a-zA-Z_0-9]+"
    )
    val loginName: String?,
    @field:NotNull
    @field:Email
    // @Email accepts an empty string and an address longer than the 254-character column. @Size refuses both as a
    // field error, before the domain's own check on the address.
    @field:Size(
        min = User.MIN_EMAIL_LENGTH,
        max = User.MAX_EMAIL_LENGTH,
        message = "Email address must be between {min} and {max} characters long."
    )
    val emailAddress: String?,
    @field:NotNull
    @field:Size(min = 1, max = 255, message = "First name must be between 1 and 255 characters long.")
    val firstName: String?,
    @field:NotNull
    @field:Size(min = 1, max = 255, message = "Last name must be between 1 and 255 characters long.")
    val lastName: String?
) : Dto<UUID>
