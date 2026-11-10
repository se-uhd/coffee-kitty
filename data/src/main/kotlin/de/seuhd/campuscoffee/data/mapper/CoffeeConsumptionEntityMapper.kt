package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CoffeeConsumption] domain objects and [CoffeeConsumptionEntity] JPA entities. [toEntity]
 * and [updateEntity] set [CoffeeConsumptionEntity.user] to the stored user, which [EntityReferenceResolver] loads by
 * the id of [CoffeeConsumption.user]. Saving a consumption then leaves the user row unchanged (see
 * [EntityReferenceResolver]). [fromEntity] maps the user with all fields through [UserEntityMapper]. [updateEntity]
 * does not copy the id or the timestamps (see [EntityMapper.updateEntity]).
 */
@Mapper(componentModel = "spring", uses = [UserEntityMapper::class, EntityReferenceResolver::class])
interface CoffeeConsumptionEntityMapper : EntityMapper<CoffeeConsumption, CoffeeConsumptionEntity> {
    @Mapping(target = "user", source = "user.id")
    override fun toEntity(source: CoffeeConsumption): CoffeeConsumptionEntity

    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: CoffeeConsumption,
        @MappingTarget target: CoffeeConsumptionEntity
    )
}
