package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper from a [CoffeeConsumption] domain object to its response DTO (one-way), flattening the user to
 * their login name. A request that changes the count carries only a delta, with no DTO to map back into the domain.
 */
@Mapper(componentModel = "spring")
interface ConsumptionDtoMapper {
    /**
     * Maps a coffee consumption to its response DTO.
     *
     * @param consumption the consumption to map
     */
    @Mapping(target = "loginName", source = "user.loginName")
    fun toDto(consumption: CoffeeConsumption): ConsumptionDto
}
