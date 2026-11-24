package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeRatingEntity
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * MapStruct mapper between [CoffeeRating] domain objects and [CoffeeRatingEntity] JPA entities. [toEntity] and
 * [updateEntity] set [CoffeeRatingEntity.user] and [CoffeeRatingEntity.bean] to the stored user and bean, which
 * [EntityReferenceResolver] loads by the ids of [CoffeeRating.user] and [CoffeeRating.bean]. Saving a rating then
 * leaves the user row and the bean row unchanged. [fromEntity] maps the user through [UserEntityMapper] and the bean
 * through [CoffeeBeanEntityMapper]. [updateEntity] does not copy the id or the timestamps (see
 * [EntityMapper.updateEntity]).
 */
@Mapper(
    componentModel = "spring",
    uses = [UserEntityMapper::class, CoffeeBeanEntityMapper::class, EntityReferenceResolver::class]
)
interface CoffeeRatingEntityMapper : EntityMapper<CoffeeRating, CoffeeRatingEntity> {
    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "bean", source = "bean.id")
    override fun toEntity(source: CoffeeRating): CoffeeRatingEntity

    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "bean", source = "bean.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: CoffeeRating,
        @MappingTarget target: CoffeeRatingEntity
    )
}
