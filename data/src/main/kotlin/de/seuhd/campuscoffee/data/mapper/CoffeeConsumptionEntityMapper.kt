package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CoffeeConsumption] domain objects and [CoffeeConsumptionEntity] persistence entities.
 * [toEntity] and [updateEntity] set the [CoffeeConsumption.user] association to the stored user with the same id
 * through the [EntityReferenceResolver], so saving a consumption sets only the foreign key and never rewrites the
 * referenced user row. [fromEntity] maps the user in full through the [UserEntityMapper]. As in the
 * [UserEntityMapper], [updateEntity] does not copy the id or the timestamps.
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
