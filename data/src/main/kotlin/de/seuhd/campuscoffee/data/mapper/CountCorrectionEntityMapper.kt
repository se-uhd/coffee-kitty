package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CountCorrectionEntity
import de.seuhd.campuscoffee.domain.model.CountCorrection
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CountCorrection] domain objects and [CountCorrectionEntity] JPA entities. [toEntity] and
 * [updateEntity] set [CountCorrectionEntity.user] and [CountCorrectionEntity.admin] to the stored users, which
 * [EntityReferenceResolver] loads by the ids of [CountCorrection.user] and [CountCorrection.admin]. Saving a
 * correction then leaves both user rows unchanged. [fromEntity] maps both users with all fields through
 * [UserEntityMapper]. [updateEntity] does not copy the id or the timestamps (see [EntityMapper.updateEntity]).
 */
@Mapper(componentModel = "spring", uses = [UserEntityMapper::class, EntityReferenceResolver::class])
interface CountCorrectionEntityMapper : EntityMapper<CountCorrection, CountCorrectionEntity> {
    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "admin", source = "admin.id")
    override fun toEntity(source: CountCorrection): CountCorrectionEntity

    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "admin", source = "admin.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: CountCorrection,
        @MappingTarget target: CountCorrectionEntity
    )
}
