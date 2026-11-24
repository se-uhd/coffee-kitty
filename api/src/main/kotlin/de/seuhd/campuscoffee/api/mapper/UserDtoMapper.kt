package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper between a [User] and a [UserDto]. Every property has the same name in both classes. The kapt
 * convention plugin sets `mapstruct.unmappedTargetPolicy` to `ERROR`. MapStruct therefore stops the build when one
 * class gets a property that the other class does not have. [toDomain] sets a missing role to `USER`, because the role
 * of a [User] cannot be null.
 */
@Mapper(componentModel = "spring")
interface UserDtoMapper : DtoMapper<User, UserDto> {
    @Mapping(target = "role", source = "role", defaultValue = "USER")
    override fun toDomain(source: UserDto): User
}
