package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeePriceEntity
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CoffeePrice] domain objects and [CoffeePriceEntity] JPA entities. MapStruct maps each field
 * to the field with the same name. [updateEntity] does not copy the id or the timestamps (see
 * [EntityMapper.updateEntity]).
 */
@Mapper(componentModel = "spring")
interface CoffeePriceEntityMapper : EntityMapper<CoffeePrice, CoffeePriceEntity> {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: CoffeePrice,
        @MappingTarget target: CoffeePriceEntity
    )
}
