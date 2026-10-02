package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [User] domain objects and [UserEntity] persistence entities. The fields map by name in
 * both directions. [updateEntity] does not copy the id or the timestamps, because the id names the stored row and the
 * entity's lifecycle callbacks set the timestamps.
 */
@Mapper(componentModel = "spring")
interface UserEntityMapper : EntityMapper<User, UserEntity> {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: User,
        @MappingTarget target: UserEntity
    )
}
