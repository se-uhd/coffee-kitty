package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeBeanEntity
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CoffeeBean] domain objects and [CoffeeBeanEntity] JPA entities. MapStruct maps each field
 * to the field with the same name. It maps [CoffeeBean.mergedIntoId] in the same way, because this field is a plain id.
 * [updateEntity] does not copy the id or the timestamps (see [EntityMapper.updateEntity]).
 */
@Mapper(componentModel = "spring")
interface CoffeeBeanEntityMapper : EntityMapper<CoffeeBean, CoffeeBeanEntity> {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: CoffeeBean,
        @MappingTarget target: CoffeeBeanEntity
    )
}
