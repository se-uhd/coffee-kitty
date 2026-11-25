package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.CreateUserDto
import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * The MapStruct mapper between a [User] and its DTOs. With separate DTOs, the domain models do not need API
 * annotations, such as `@Schema`. The kapt convention plugin sets `mapstruct.unmappedTargetPolicy` to
 * `ERROR`. MapStruct therefore stops the build when a target class gets a property that the source class does not have.
 */
@Mapper(componentModel = "spring")
interface UserDtoMapper {
    /**
     * Converts a stored [user] to its response body. A stored user has an id and timestamps, which are non-null in
     * [UserDto].
     *
     * @param user the stored user to convert
     */
    fun toDto(user: User): UserDto

    /**
     * Converts the request body [dto] to a [User] without an id and timestamps, which the data module assigns. The
     * mapper sets a missing role to `USER`, because the role of a [User] cannot be null.
     *
     * @param dto the request body
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", source = "role", defaultValue = "USER")
    fun toDomain(dto: CreateUserDto): User
}
