package de.seuhd.coffeekitty.api.mapper

import de.seuhd.coffeekitty.api.dtos.UserDto
import de.seuhd.coffeekitty.domain.model.User
import org.mapstruct.Mapper

/**
 * MapStruct mapper between [User] domain objects and [UserDto]s. Every property maps by name in both directions, so
 * the mapper declares no `@Mapping`.
 */
@Mapper(componentModel = "spring")
interface UserDtoMapper : DtoMapper<User, UserDto>
