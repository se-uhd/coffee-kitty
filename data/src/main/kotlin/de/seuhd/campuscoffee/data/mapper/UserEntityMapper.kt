package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [User] domain objects and [UserEntity] JPA entities. MapStruct maps each field to the field
 * with the same name. [updateEntity] does not copy the id or the timestamps (see [EntityMapper.updateEntity]).
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
