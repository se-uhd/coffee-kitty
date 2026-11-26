package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import io.swagger.v3.oas.annotations.media.Schema

/**
 * The request body of `POST /api/users`, the fields of a new user. Jackson rejects a body with a field that this class
 * does not have, such as `id`, because `application.yaml` sets
 * `spring.jackson.deserialization.fail-on-unknown-properties` to true. A property that is not nullable is a required
 * field (ADR-008). The domain checks the values, and the `@Schema` annotations only document their limits (ADR-009).
 * `UserDtoMapper` sets a missing [role] to `USER`.
 */
data class CreateUserDto(
    @field:Schema(
        description = "The unique login name: ASCII letters, digits, and underscores.",
        maxLength = User.MAX_LOGIN_NAME_LENGTH,
        pattern = User.LOGIN_NAME_PATTERN,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val loginName: String,
    @field:Schema(
        description =
            "The unique email address of the form name@domain, without control characters. The server stores it in " +
                "lowercase.",
        maxLength = User.MAX_EMAIL_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val emailAddress: String,
    @field:Schema(
        description = "The first name, which must not be blank or contain a control character.",
        maxLength = User.MAX_NAME_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val firstName: String,
    @field:Schema(
        description = "The last name, which must not be blank or contain a control character.",
        maxLength = User.MAX_NAME_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val lastName: String,
    @field:Schema(description = "The role of the user. A request without a role creates a USER.")
    val role: Role? = null
)
